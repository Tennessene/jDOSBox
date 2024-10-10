package jdos.ints;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.dos.Dos_tables;
import jdos.gui.Main;
import jdos.hardware.*;
import jdos.misc.Log;
import jdos.misc.setup.Section;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;
import jdos.util.ShortRef;

import java.io.*;

public class Mouse {
    static private /*Bitu*/int call_int33,call_int74,int74_ret_callback,call_mouse_bd;
    static private /*Bit16u*/int ps2cbseg,ps2cbofs;
    static private boolean useps2callback,ps2callbackinit;
    static private /*Bitu*/int call_ps2;
    static private /*RealPt*/int ps2_callback;
    static private /*Bit16s*/short oldmouseX, oldmouseY;

    private static class button_event {
        /*Bit8u*/short type;
        /*Bit8u*/short buttons;
    }

    static private final int QUEUE_SIZE = 32;
    static private final int MOUSE_BUTTONS = 3;
    static private final int MOUSE_IRQ = 12;

    static public short POS_X() {
        return ((/*Bit16s*/short)((short)mouse.x & mouse.gran_x));
    }

    static public short POS_Y() {
        return ((/*Bit16s*/short)((short)mouse.y & mouse.gran_y));
    }

    static private final int CURSORX = 16;
    static private final int CURSORY = 16;
    static private final int HIGHESTBIT = (1<<(CURSORX-1));

    static private final /*Bit16u*/int defaultTextAndMask = 0x77FF;
    static private final /*Bit16u*/int defaultTextXorMask = 0x7700;

    static private final /*Bit16u*/int[] defaultScreenMask = {
            0x3FFF, 0x1FFF, 0x0FFF, 0x07FF,
            0x03FF, 0x01FF, 0x00FF, 0x007F,
            0x003F, 0x001F, 0x01FF, 0x00FF,
            0x30FF, 0xF87F, 0xF87F, 0xFCFF
    };

    static private final /*Bit16u*/int[] defaultCursorMask = {
		0x0000, 0x4000, 0x6000, 0x7000,
		0x7800, 0x7C00, 0x7E00, 0x7F00,
		0x7F80, 0x7C00, 0x6C00, 0x4600,
		0x0600, 0x0300, 0x0300, 0x0000
    };

    static private /*Bit16u*/int[] userdefScreenMask = new int[CURSORY];
    static private /*Bit16u*/int[] userdefCursorMask = new int[CURSORY];

    static private void write(DataOutputStream dos, int i) throws IOException {
        dos.writeInt(i);
    }
    static private void write(DataOutputStream dos, boolean i) throws IOException {
        dos.writeBoolean(i);
    }
    static private void write(DataOutputStream dos, short s) throws IOException {
        dos.writeShort(s);
    }
    static private void write(DataOutputStream dos, int[] a) throws IOException {
        for (int i=0;i<a.length;i++) dos.writeInt(a[i]);
    }

    static public class _mouse {
        static private int[][] maskholder = new int[128][];

        public _mouse() {
            for (int i=0;i<event_queue.length;i++)
                event_queue[i] = new button_event();
        }
        private void write16u(DataOutputStream os, int[] d) throws IOException {
            for (int i=0;i<d.length;i++) {
                os.writeShort(d[i] & 0xFFFF);
            }
        }
        private void write16u(DataOutputStream os, int d) throws IOException {
            os.writeShort(d & 0xFFFF);
        }
        private void write16s(DataOutputStream os, short d) throws IOException {
            os.writeShort(d);
        }
        private void write(DataOutputStream os, float d) throws IOException {
            os.writeFloat(d);
        }
        private void write8u(DataOutputStream os, short d) throws IOException {
            os.write(d & 0xFF);
        }
        private void write8u(DataOutputStream os, short[] d) throws IOException {
            for (int i=0;i<d.length;i++)
                os.write(d[i] & 0xFF);
        }
        private void writeBool(DataOutputStream os, boolean b) throws IOException {
            os.writeBoolean(b);
        }
        private void write16uPtr(DataOutputStream os, int[] d) throws IOException {
            for (int i=1;i<maskholder.length;i++) {
                if (maskholder[i] == null) {
                    maskholder[i] = d;
                    os.writeInt(i);
                    return;
                }
            }
            Log.exit("Failed to save mouse state");
        }

        private void read16u(DataInputStream is, int[] d) throws IOException {
            for (int i=0;i<d.length;i++) {
                d[i] = is.readUnsignedShort();
            }
        }
        private int read16u(DataInputStream is) throws IOException {
            return is.readUnsignedShort();
        }
        private short read16s(DataInputStream is) throws IOException {
            return is.readShort();
        }
        private float readFloat(DataInputStream is) throws IOException {
            return is.readFloat();
        }
        private short read8u(DataInputStream is) throws IOException {
            return (short)is.readUnsignedByte();
        }
        private void read8u(DataInputStream is, short[] d) throws IOException {
            for (int i=0;i<d.length;i++)
                d[i] = (short)is.readUnsignedByte();
        }
        private boolean readBool(DataInputStream is) throws IOException {
            return is.readBoolean();
        }
        private int[] read16uPtr(DataInputStream is) throws IOException {
            int index = is.readInt();
            int[] result = maskholder[index];
            maskholder[index] = null;
            return result;
        }
        public byte[] save() {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream os = new DataOutputStream(bos);
                write8u(os, buttons);
                write16u(os, times_pressed);
                write16u(os, times_released);
                write16u(os, last_released_x);
                write16u(os, last_released_y);
                write16u(os, last_pressed_x);
                write16u(os, last_pressed_y);
                write16u(os, hidden);
                write(os, add_x);
                write(os, add_y);
                write16s(os, min_x);
                write16s(os, max_x);
                write16s(os, min_y);
                write16s(os, max_y);
                write(os, mickey_x);
                write(os, mickey_y);
                write(os, x);
                write(os, y);
                for (int i=0;i<event_queue.length;i++) {
                    write8u(os, event_queue[i].type);
                    write8u(os, event_queue[i].buttons);
                }
                write8u(os, events);
                write16u(os, sub_seg);
                write16u(os, sub_ofs);
                write16u(os, sub_mask);
                writeBool(os, background);
                write16s(os, backposx);
                write16s(os, backposy);
                write8u(os, backData);
                write16uPtr(os, screenMask);
                write16uPtr(os, cursorMask);
                write16s(os, clipx);
                write16s(os, clipy);
                write16s(os, hotx);
                write16s(os, hoty);
                write16u(os, textAndMask);
                write16u(os, textXorMask);

                write(os, mickeysPerPixel_x);
                write(os, mickeysPerPixel_y);
                write(os, pixelPerMickey_x);
                write(os, pixelPerMickey_y);
                write16u(os, senv_x_val);
                write16u(os, senv_y_val);
                write16u(os, dspeed_val);
                write(os, senv_x);
                write(os, senv_y);
                write16u(os, updateRegion_x);
                write16u(os, updateRegion_y);
                write16u(os, doubleSpeedThreshold);
                write16u(os, language);
                write16u(os, cursorType);
                write16u(os, oldhidden);
                write8u(os, page);
                writeBool(os, enabled);
                writeBool(os, inhibit_draw);
                writeBool(os, timer_in_progress);
                writeBool(os, in_UIR);
                write8u(os, mode);
                write16s(os, gran_x);
                write16s(os, gran_y);
                return bos.toByteArray();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new byte[0];
        }

        public void load(byte[] data) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(data);
                DataInputStream is = new DataInputStream(bis);
                buttons = read8u(is);
                read16u(is, times_pressed);
                read16u(is, times_released);
                read16u(is, last_released_x);
                read16u(is, last_released_y);
                read16u(is, last_pressed_x);
                read16u(is, last_pressed_y);
                hidden = read16u(is);
                add_x = readFloat(is);
                add_y = readFloat(is);
                min_x = read16s(is);
                max_x = read16s(is);
                min_y = read16s(is);
                max_y = read16s(is);
                mickey_x = readFloat(is);
                mickey_y = readFloat(is);
                x = readFloat(is);
                x = readFloat(is);
                for (int i=0;i<event_queue.length;i++) {
                    event_queue[i].type = read8u(is);
                    event_queue[i].buttons = read8u(is);
                }
                events = read8u(is);
                sub_seg = read16u(is);
                sub_ofs = read16u(is);
                sub_mask = read16u(is);
                background = readBool(is);
                backposx = read16s(is);
                backposy = read16s(is);
                read8u(is, backData);
                screenMask = read16uPtr(is);
                cursorMask = read16uPtr(is);
                clipx = read16s(is);
                clipy = read16s(is);
                hotx = read16s(is);
                hoty = read16s(is);
                textAndMask = read16u(is);
                textXorMask = read16u(is);

                mickeysPerPixel_x = readFloat(is);
                mickeysPerPixel_y = readFloat(is);
                pixelPerMickey_x = readFloat(is);
                pixelPerMickey_y = readFloat(is);
                senv_x_val = read16u(is);
                senv_y_val = read16u(is);
                dspeed_val = read16u(is);
                senv_x = readFloat(is);
                senv_y = readFloat(is);
                read16u(is, updateRegion_x);
                read16u(is, updateRegion_y);
                doubleSpeedThreshold = read16u(is);
                language = read16u(is);
                cursorType = read16u(is);
                oldhidden = read16u(is);
                page = read8u(is);
                enabled = readBool(is);
                inhibit_draw = readBool(is);
                timer_in_progress = readBool(is);
                in_UIR = readBool(is);
                mode = read8u(is);
                gran_x = read16s(is);
                gran_y = read16s(is);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        public static int sizeof() {
            return 484;// same as dosbox, java need 477
        }
        /*Bit8u*/short buttons;
        /*Bit16u*/int[] times_pressed = new int[MOUSE_BUTTONS];
        /*Bit16u*/int[] times_released = new int[MOUSE_BUTTONS];
        /*Bit16u*/int[] last_released_x = new int[MOUSE_BUTTONS];
        /*Bit16u*/int[] last_released_y = new int[MOUSE_BUTTONS];
        /*Bit16u*/int[] last_pressed_x = new int[MOUSE_BUTTONS];
        /*Bit16u*/int[] last_pressed_y = new int[MOUSE_BUTTONS];
        /*Bit16u*/int hidden;
        float add_x,add_y;
        public /*Bit16s*/short min_x,max_x,min_y,max_y;
        float mickey_x,mickey_y;
        public float x,y;
        button_event[] event_queue = new button_event[QUEUE_SIZE];
        /*Bit8u*/short events;//Increase if QUEUE_SIZE >255 (currently 32)
        /*Bit16u*/int sub_seg,sub_ofs;
        /*Bit16u*/int sub_mask;

        boolean	background;
        /*Bit16s*/short backposx, backposy;
        /*Bit8u*/short[] backData = new short[CURSORX*CURSORY];
        /*Bit16u*/int[] screenMask;
        /*Bit16u*/int[] cursorMask;
        /*Bit16s*/short	clipx,clipy;
        /*Bit16s*/short  hotx,hoty;
        /*Bit16u*/int  textAndMask, textXorMask;

        float	mickeysPerPixel_x;
        float	mickeysPerPixel_y;
        float	pixelPerMickey_x;
        float	pixelPerMickey_y;
        /*Bit16u*/int	senv_x_val;
        /*Bit16u*/int	senv_y_val;
        /*Bit16u*/int	dspeed_val;
        float	senv_x;
        float	senv_y;
        /*Bit16u*/int[]  updateRegion_x=new int[2];
        /*Bit16u*/int[] updateRegion_y=new int[2];
        /*Bit16u*/int  doubleSpeedThreshold;
        /*Bit16u*/int  language;
        /*Bit16u*/int  cursorType;
        /*Bit16u*/int	oldhidden;
        /*Bit8u*/short  page;
        boolean enabled;
        boolean inhibit_draw;
        boolean timer_in_progress;
        boolean in_UIR;
        /*Bit8u*/short mode;
        /*Bit16s*/short gran_x,gran_y;
    }
    static public _mouse mouse = new _mouse();

    static public boolean Mouse_SetPS2State(boolean use) {
        if (use && (!ps2callbackinit)) {
            useps2callback = false;
            Pic.PIC_SetIRQMask(MOUSE_IRQ,true);
            return false;
        }
        useps2callback = use;
        Main.Mouse_AutoLock(useps2callback);
        Pic.PIC_SetIRQMask(MOUSE_IRQ,!useps2callback);
        return true;
    }

    static public void Mouse_ChangePS2Callback(/*Bit16u*/int pseg, /*Bit16u*/int pofs) {
        if ((pseg==0) && (pofs==0)) {
            ps2callbackinit = false;
            Main.Mouse_AutoLock(false);
        } else {
            ps2callbackinit = true;
            ps2cbseg = pseg;
            ps2cbofs = pofs;
        }
        Main.Mouse_AutoLock(ps2callbackinit);
    }

    static private void DoPS2Callback(/*Bit16u*/int data, /*Bit16s*/short mouseX, /*Bit16s*/short mouseY) {
        if (useps2callback) {
            /*Bit16u*/int mdat = (data & 0x03) | 0x08;
            /*Bit16s*/int xdiff = mouseX-oldmouseX;
            /*Bit16s*/int ydiff = oldmouseY-mouseY;
            oldmouseX = mouseX;
            oldmouseY = mouseY;
            if ((xdiff>0xff) || (xdiff<-0xff)) mdat |= 0x40;		// x overflow
            if ((ydiff>0xff) || (ydiff<-0xff)) mdat |= 0x80;		// y overflow
            xdiff %= 256;
            ydiff %= 256;
            if (xdiff<0) {
                xdiff = (0x100+xdiff);
                mdat |= 0x10;
            }
            if (ydiff<0) {
                ydiff = (0x100+ydiff);
                mdat |= 0x20;
            }
            CPU.CPU_Push16(mdat);
            CPU.CPU_Push16((xdiff % 256));
            CPU.CPU_Push16((ydiff % 256));
            CPU.CPU_Push16(0);
            CPU.CPU_Push16(Memory.RealSeg(ps2_callback));
            CPU.CPU_Push16(Memory.RealOff(ps2_callback));
            CPU_Regs.SegSet16CS(ps2cbseg);
            CPU_Regs.reg_ip(ps2cbofs);
        }
    }

    static private Callback.Handler PS2_Handler = new Callback.Handler() {
        public String getName() {
            return "Mouse.PS2_Handler";
        }
        public /*Bitu*/int call() {
            CPU.CPU_Pop16();CPU.CPU_Pop16();CPU.CPU_Pop16();CPU.CPU_Pop16();// remove the 4 words
            return Callback.CBRET_NONE;
        }
    };


    static private final int X_MICKEY = 8;
    static private final int Y_MICKEY = 8;

    static private final int MOUSE_HAS_MOVED = 1;
    static private final int MOUSE_LEFT_PRESSED = 2;
    static private final int MOUSE_LEFT_RELEASED = 4;
    static private final int MOUSE_RIGHT_PRESSED = 8;
    static private final int MOUSE_RIGHT_RELEASED = 16;
    static private final int MOUSE_MIDDLE_PRESSED = 32;
    static private final int MOUSE_MIDDLE_RELEASED = 64;
    static private final float MOUSE_DELAY = 5.0f;

    static private Pic.PIC_EventHandler MOUSE_Limit_Events = new Pic.PIC_EventHandler() {
        public void call(/*Bitu*/int val) {
            mouse.timer_in_progress = false;
            if (mouse.events!=0) {
                mouse.timer_in_progress = true;
                Pic.PIC_AddEvent(MOUSE_Limit_Events,MOUSE_DELAY);
                Pic.PIC_ActivateIRQ(MOUSE_IRQ);
            }
        }
        public String toString() {
            return "MOUSE_Limit_Events";
        }
    };

    static private void Mouse_AddEvent(/*Bit8u*/int type) {
        if (mouse.events<QUEUE_SIZE) {
            if (mouse.events>0) {
                /* Skip duplicate events */
                if (type==MOUSE_HAS_MOVED) return;
                /* Always put the newest element in the front as that the events are
                 * handled backwards (prevents doubleclicks while moving)
                 */
                for(/*Bitu*/int i = mouse.events ; i!=0 ; i--)
                    mouse.event_queue[i] = mouse.event_queue[i-1];
            }
            mouse.event_queue[0].type=(short)type;
            mouse.event_queue[0].buttons=mouse.buttons;
            mouse.events++;
        }
        if (!mouse.timer_in_progress) {
            mouse.timer_in_progress = true;
            Pic.PIC_AddEvent(MOUSE_Limit_Events,MOUSE_DELAY);
            Pic.PIC_ActivateIRQ(MOUSE_IRQ);
        }
    }

    // ***************************************************************************
    // Mouse cursor - text mode
    // ***************************************************************************
    /* Write and read directly to the screen. Do no use int_setcursorpos (LOTUS123) */

    private static void RestoreCursorBackgroundText() {
        if (mouse.hidden!=0 || mouse.inhibit_draw) return;

        if (mouse.background) {
            Int10_char.WriteChar(mouse.backposx,mouse.backposy,(short)Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAGE),mouse.backData[0],mouse.backData[1],true);
            mouse.background = false;
        }
    }

    private static void DrawCursorText() {
        // Restore Background
        RestoreCursorBackgroundText();


        // Save Background
        mouse.backposx		= (short)(POS_X()>>>3);
        mouse.backposy		= (short)(POS_Y()>>>3);
        if (mouse.mode < 2) mouse.backposx >>= 1;

        //use current page (CV program)
        /*Bit8u*/short page = (short)Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAGE);
        /*Bit16u*/int result;

        result=Int10_char.ReadCharAttr(mouse.backposx,mouse.backposy,page);
        mouse.backData[0]	= (/*Bit8u*/short)(result & 0xFF);
        mouse.backData[1]	= (/*Bit8u*/short)(result>>8);
        mouse.background	= true;
        // Write Cursor
        result = (result & mouse.textAndMask) ^ mouse.textXorMask;
        Int10_char.WriteChar(mouse.backposx,mouse.backposy,page,(/*Bit8u*/short)(result&0xFF),(/*Bit8u*/short)(result>>8),true);
    }

    // ***************************************************************************
    // Mouse cursor - graphic mode
    // ***************************************************************************

    static /*Bit8u*/short[] gfxReg3CE=new short[9];
    static /*Bit8u*/short index3C4,gfxReg3C5;
    private static void SaveVgaRegisters() {
        if (Dosbox.IS_VGA_ARCH()) {
            for (/*Bit8u*/short i=0; i<9; i++) {
                IoHandler.IO_Write	(0x3CE,i);
                gfxReg3CE[i] = IoHandler.IO_Read(0x3CF);
            }
            /* Setup some default values in GFX regs that should work */
            IoHandler.IO_Write(0x3CE,3); IoHandler.IO_Write(0x3Cf,0);				//disable rotate and operation
            IoHandler.IO_Write(0x3CE,5); IoHandler.IO_Write(0x3Cf,gfxReg3CE[5]&0xf0);	//Force read/write mode 0

            //Set Map to all planes. Celtic Tales
            index3C4 = IoHandler.IO_Read(0x3c4);  IoHandler.IO_Write(0x3C4,2);
            gfxReg3C5 = IoHandler.IO_Read(0x3C5); IoHandler.IO_Write(0x3C5,0xF);
        } else if (Dosbox.machine== MachineType.MCH_EGA) {
            //Set Map to all planes.
            IoHandler.IO_Write(0x3C4,2);
            IoHandler.IO_Write(0x3C5,0xF);
        }
    }

    private static void RestoreVgaRegisters() {
        if (Dosbox.IS_VGA_ARCH()) {
            for (/*Bit8u*/short i=0; i<9; i++) {
                IoHandler.IO_Write(0x3CE,i);
                IoHandler.IO_Write(0x3CF,gfxReg3CE[i]);
            }

            IoHandler.IO_Write(0x3C4,2);
            IoHandler.IO_Write(0x3C5,gfxReg3C5);
            IoHandler.IO_Write(0x3C4,index3C4);
        }
    }

    private static void ClipCursorArea(/*Bit16s*/ShortRef x1, /*Bit16s*/ShortRef x2, /*Bit16s*/ShortRef y1, /*Bit16s*/ShortRef y2,
                        /*Bit16u*/ShortRef addx1, /*Bit16u*/ShortRef addx2, /*Bit16u*/ShortRef addy) {
        addx1.value = addx2.value = addy.value = 0;
        // Clip up
        if (y1.value<0) {
            addy.value += (-y1.value);
            y1.value = 0;
        }
        // Clip down
        if (y2.value>mouse.clipy) {
            y2.value = mouse.clipy;
        }
        // Clip left
        if (x1.value<0) {
            addx1.value += (-x1.value);
            x1.value = 0;
        }
        // Clip right
        if (x2.value>mouse.clipx) {
            addx2.value = (short)(x2.value - mouse.clipx);
            x2.value = mouse.clipx;
        }
    }

    static private void RestoreCursorBackground() {
        if (mouse.hidden!=0 || mouse.inhibit_draw) return;

        SaveVgaRegisters();
        if (mouse.background) {
            // Restore background
            /*Bit16s*/short x,y;
            /*Bit16u*/ShortRef addx1=new ShortRef(),addx2=new ShortRef(),addy=new ShortRef();
            /*Bit16u*/int dataPos	= 0;
            /*Bit16s*/ShortRef x1 = new ShortRef(mouse.backposx);
            /*Bit16s*/ShortRef y1 = new ShortRef(mouse.backposy);
            /*Bit16s*/ShortRef x2 = new ShortRef(x1.value + CURSORX - 1);
            /*Bit16s*/ShortRef y2 = new ShortRef(y1.value + CURSORY - 1);

            ClipCursorArea(x1, x2, y1, y2, addx1, addx2, addy);

            dataPos = addy.value * CURSORX;
            for (y=y1.value; y<=y2.value; y++) {
                dataPos += addx1.value;
                for (x=x1.value; x<=x2.value; x++) {
                    Int10_put_pixel.INT10_PutPixel(x,y,mouse.page,mouse.backData[dataPos++]);
                }
                dataPos += addx2.value;
            };
            mouse.background = false;
        }
        RestoreVgaRegisters();
    }

    static private void DrawCursor() {
        if (mouse.hidden!=0 || mouse.inhibit_draw) return;
        // In Textmode ?
        if (Int10_modes.CurMode.type== VGA.M_TEXT) {
            DrawCursorText();
            return;
        }

        // Check video page. Seems to be ignored for text mode.
        // hence the text mode handled above this
        if (Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_CURRENT_PAGE)!=mouse.page) return;
    // Check if cursor in update region
    /*	if ((POS_X >= mouse.updateRegion_x[0]) && (POS_X <= mouse.updateRegion_x[1]) &&
            (POS_Y >= mouse.updateRegion_y[0]) && (POS_Y <= mouse.updateRegion_y[1])) {
            if (CurMode.type==M_TEXT16)
                RestoreCursorBackgroundText();
            else
                RestoreCursorBackground();
            mouse.shown--;
            return;
        }
       */ /*Not sure yet what to do update region should be set to ??? */

        // Get Clipping ranges


        mouse.clipx = (/*Bit16s*/short)(Int10_modes.CurMode.swidth-1);	/* Get from bios ? */
        mouse.clipy = (/*Bit16s*/short)(Int10_modes.CurMode.sheight-1);

        /* might be vidmode == 0x13?2:1 */
        /*Bit16s*/short xratio = 640;
        if (Int10_modes.CurMode.swidth>0) xratio/=Int10_modes.CurMode.swidth;
        if (xratio==0) xratio = 1;

        RestoreCursorBackground();

        SaveVgaRegisters();

        // Save Background
        /*Bit16s*/short x,y;
        /*Bit16u*/ShortRef addx1 = new ShortRef(),addx2 = new ShortRef(),addy = new ShortRef();
        /*Bit16u*/int dataPos	= 0;
        /*Bit16s*/ShortRef x1		= new ShortRef(POS_X() / xratio - mouse.hotx);
        /*Bit16s*/ShortRef y1		= new ShortRef(POS_Y() - mouse.hoty);
        /*Bit16s*/ShortRef x2		= new ShortRef(x1.value + CURSORX - 1);
        /*Bit16s*/ShortRef y2		= new ShortRef(y1.value + CURSORY - 1);

        ClipCursorArea(x1,x2,y1,y2, addx1, addx2, addy);

        dataPos = addy.value * CURSORX;
        for (y=y1.value; y<=y2.value; y++) {
            dataPos += addx1.value;
            for (x=x1.value; x<=x2.value; x++) {
                mouse.backData[dataPos++] = Int10_put_pixel.INT10_GetPixel(x,y,mouse.page);
            }
            dataPos += addx2.value;
        }
        mouse.background= true;
        mouse.backposx	= (short)(POS_X() / xratio - mouse.hotx);
        mouse.backposy	= (short)(POS_Y() - mouse.hoty);

        // Draw Mousecursor
        dataPos = addy.value * CURSORX;
        for (y=y1.value; y<=y2.value; y++) {
            /*Bit16u*/int scMask = mouse.screenMask[addy.value+y-y1.value];
            /*Bit16u*/int cuMask = mouse.cursorMask[addy.value+y-y1.value];
            if (addx1.value>0) { scMask<<=addx1.value; cuMask<<=addx1.value; dataPos += addx1.value; };
            for (x=x1.value; x<=x2.value; x++) {
                /*Bit8u*/short pixel = 0;
                // ScreenMask
                if ((scMask & HIGHESTBIT)!=0) pixel = mouse.backData[dataPos];
                scMask<<=1;
                // CursorMask
                if ((cuMask & HIGHESTBIT)!=0) pixel = (short)(pixel ^ 0x0F);
                cuMask<<=1;
                // Set Pixel
                Int10_put_pixel.INT10_PutPixel(x,y,mouse.page,pixel);
                dataPos++;
            }
            dataPos += addx2.value;
        }
        RestoreVgaRegisters();
    }

    static public void Mouse_CursorMoved(float xrel,float yrel,float x,float y,boolean emulate) {
        float dx = xrel * mouse.pixelPerMickey_x;
        float dy = yrel * mouse.pixelPerMickey_y;

        if (Keyboard.KEYBOARD_AUX_Active()) {
            Keyboard.KEYBOARD_AUX_Event(xrel,yrel,mouse.buttons);
            return;
        }
        if((Math.abs(xrel) > 1.0) || (mouse.senv_x < 1.0)) dx *= mouse.senv_x;
        if((Math.abs(yrel) > 1.0) || (mouse.senv_y < 1.0)) dy *= mouse.senv_y;
        if (useps2callback) dy *= 2;

        mouse.mickey_x += (dx * mouse.mickeysPerPixel_x);
        mouse.mickey_y += (dy * mouse.mickeysPerPixel_y);
        if (mouse.mickey_x >= 32768.0) mouse.mickey_x -= 65536.0;
        else if (mouse.mickey_x <= -32769.0) mouse.mickey_x += 65536.0;
        if (mouse.mickey_y >= 32768.0) mouse.mickey_y -= 65536.0;
        else if (mouse.mickey_y <= -32769.0) mouse.mickey_y += 65536.0;

        if (emulate) {
            mouse.x += dx;
            mouse.y += dy;
        } else {
            if (Int10_modes.CurMode.type == VGA.M_TEXT) {
                mouse.x = x*Int10_modes.CurMode.swidth;
                mouse.y = y*Int10_modes.CurMode.sheight * 8 / Int10_modes.CurMode.cheight;
            } else if ((mouse.max_x < 2048) || (mouse.max_y < 2048) || (mouse.max_x != mouse.max_y)) {
                if ((mouse.max_x > 0) && (mouse.max_y > 0)) {
                    mouse.x = x*mouse.max_x;
                    mouse.y = y*mouse.max_y;
                } else {
                    mouse.x += xrel;
                    mouse.y += yrel;
                }
            } else { // Games faking relative movement through absolute coordinates. Quite surprising that this actually works..
                mouse.x += xrel;
                mouse.y += yrel;
            }
        }

        /* ignore constraints if using PS2 mouse callback in the bios */

        if (!useps2callback) {
            if (mouse.x > mouse.max_x) mouse.x = mouse.max_x;
            if (mouse.x < mouse.min_x) mouse.x = mouse.min_x;
            if (mouse.y > mouse.max_y) mouse.y = mouse.max_y;
            if (mouse.y < mouse.min_y) mouse.y = mouse.min_y;
        } else {
            if (mouse.x >= 32768.0) mouse.x -= 65536.0;
            else if (mouse.x <= -32769.0) mouse.x += 65536.0;
            if (mouse.y >= 32768.0) mouse.y -= 65536.0;
            else if (mouse.y <= -32769.0) mouse.y += 65536.0;
        }
        Mouse_AddEvent(MOUSE_HAS_MOVED);
        DrawCursor();
    }

    static private void Mouse_CursorSet(float x,float y) {
        mouse.x=x;
        mouse.y=y;
        DrawCursor();
    }

    static public void Mouse_ButtonPressed(/*Bit8u*/int button) {
        if (Keyboard.KEYBOARD_AUX_Active()) {
            switch (button) {
                case 0:
                    mouse.buttons|=1;
                    break;
                case 1:
                    mouse.buttons|=2;
                    break;
                case 2:
                    mouse.buttons|=4;
                    break;
                default:
                    return;
            }

            Keyboard.KEYBOARD_AUX_Event(0,0,mouse.buttons);
            return;
        }
        switch (button) {
        case 0:
            if (MOUSE_BUTTONS >= 1) {
                mouse.buttons|=1;
                Mouse_AddEvent(MOUSE_LEFT_PRESSED);
                break;
            }
            return;
        case 1:
            if (MOUSE_BUTTONS >= 2) {
                mouse.buttons|=2;
                Mouse_AddEvent(MOUSE_RIGHT_PRESSED);
                break;
            }
            return;
        case 2:
            if (MOUSE_BUTTONS >= 3) {
                mouse.buttons|=4;
                Mouse_AddEvent(MOUSE_MIDDLE_PRESSED);
                break;
            }
            return;
        default:
            return;
        }
        mouse.times_pressed[button]++;
        mouse.last_pressed_x[button]=POS_X();
        mouse.last_pressed_y[button]=POS_Y();
    }

    static public void Mouse_ButtonReleased(/*Bit8u*/int button) {
        if (Keyboard.KEYBOARD_AUX_Active()) {
            switch (button) {
                case 0:
                    mouse.buttons&=~1;
                    break;
                case 1:
                    mouse.buttons&=~2;
                    break;
                case 2:
                    mouse.buttons&=~4;
                    break;
                default:
                    return;
            }

            Keyboard.KEYBOARD_AUX_Event(0,0,mouse.buttons);
            return;
        }
        switch (button) {
        case 0:
            if (MOUSE_BUTTONS >= 1) {
                mouse.buttons&=~1;
                Mouse_AddEvent(MOUSE_LEFT_RELEASED);
                break;
            }
            return;
        case 1:
            if (MOUSE_BUTTONS >= 2) {
                mouse.buttons&=~2;
                Mouse_AddEvent(MOUSE_RIGHT_RELEASED);
                break;
            }
            return;
        case 2:
            if (MOUSE_BUTTONS >= 3) {
                mouse.buttons&=~4;
                Mouse_AddEvent(MOUSE_MIDDLE_RELEASED);
                break;
            }
            return;
        default:
            return;
        }
        mouse.times_released[button]++;
        mouse.last_released_x[button]=POS_X();
        mouse.last_released_y[button]=POS_Y();
    }

    static private void Mouse_SetMickeyPixelRate(/*Bit16s*/int px, /*Bit16s*/int py){
        if ((px!=0) && (py!=0)) {
            mouse.mickeysPerPixel_x	 = (float)px/X_MICKEY;
            mouse.mickeysPerPixel_y  = (float)py/Y_MICKEY;
            mouse.pixelPerMickey_x	 = X_MICKEY/(float)px;
            mouse.pixelPerMickey_y 	 = Y_MICKEY/(float)py;
        }
    }

    static private void Mouse_SetSensitivity(/*Bit16u*/int px, /*Bit16u*/int py, /*Bit16u*/int dspeed){
        if(px>100) px=100;
        if(py>100) py=100;
        if(dspeed>100) dspeed=100;
        // save values
        mouse.senv_x_val=px;
        mouse.senv_y_val=py;
        mouse.dspeed_val=dspeed;
        if ((px!=0) && (py!=0)) {
            px--;  //Inspired by cutemouse
            py--;  //Although their cursor update routine is far more complex then ours
            mouse.senv_x=((float)(px)*px)/3600.0f +1.0f/3.0f;
            mouse.senv_y=((float)(py)*py)/3600.0f +1.0f/3.0f;
         }
    }


    static private void Mouse_ResetHardware(){
        Pic.PIC_SetIRQMask(MOUSE_IRQ,false);
    }

    //Does way to much. Many things should be moved to mouse reset one day
    static public void Mouse_NewVideoMode() {
        mouse.inhibit_draw=false;
        /* Get the correct resolution from the current video mode */
        /*Bit8u*/int mode=Memory.mem_readb(Bios.BIOS_VIDEO_MODE);
        if(mode == mouse.mode) {if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_MOUSE, LogSeverities.LOG_NORMAL,"New video is the same as the old"); /*return;*/}
        mouse.gran_x = mouse.gran_y = (short)0xffff;
        switch (mode) {
        case 0x00:
        case 0x01:
        case 0x02:
        case 0x03:
        case 0x07: {
            mouse.gran_x = (short)((mode<2)?0xfff0:0xfff8);
            mouse.gran_y = (short)0xfff8;
            /*Bitu*/int rows = Memory.real_readb(Int10.BIOSMEM_SEG,Int10.BIOSMEM_NB_ROWS);
            if ((rows == 0) || (rows > 250)) rows = 25 - 1;
            mouse.max_y = (short)(8*(rows+1) - 1);
		    break;
        }
        case 0x04:
        case 0x05:
        case 0x06:
        case 0x08:
        case 0x09:
        case 0x0a:
        case 0x0d:
        case 0x0e:
        case 0x13:
            if (mode == 0x0d || mode == 0x13) mouse.gran_x = (short)0xfffe;
		    mouse.max_y = 199;
            break;
        case 0x0f:
        case 0x10:
            mouse.max_y=349;
            break;
        case 0x11:
        case 0x12:
            mouse.max_y=479;
            break;
        default:
            if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MOUSE,LogSeverities.LOG_ERROR,"Unhandled videomode "+Integer.toString(mode, 16)+" on reset");
            mouse.inhibit_draw=true;
            return;
        }
        mouse.mode = (short)mode;
        mouse.hidden = 1;
        mouse.max_x = 639;
        mouse.min_x = 0;
        mouse.min_y = 0;

        mouse.events = 0;
        mouse.timer_in_progress = false;
        Pic.PIC_RemoveEvents(MOUSE_Limit_Events);

        mouse.hotx		 = 0;
        mouse.hoty		 = 0;
        mouse.background = false;
        mouse.screenMask = defaultScreenMask;
        mouse.cursorMask = defaultCursorMask;
        mouse.textAndMask= defaultTextAndMask;
        mouse.textXorMask= defaultTextXorMask;
        mouse.language   = 0;
        mouse.page               = 0;
        mouse.doubleSpeedThreshold = 64;
        mouse.updateRegion_x[0] = 1;
        mouse.updateRegion_y[0] = 1;
        mouse.updateRegion_x[1] = 1;
        mouse.updateRegion_y[1] = 1;
        mouse.cursorType = 0;
        mouse.enabled=true;
        mouse.oldhidden=1;

        oldmouseX = (short)mouse.x;
        oldmouseY = (short)mouse.y;


    }

    //Much too empty, Mouse_NewVideoMode contains stuff that should be in here
    static private void Mouse_Reset() {
        /* Remove drawn mouse Legends of Valor */
        if (Int10_modes.CurMode.type!=VGA.M_TEXT) RestoreCursorBackground();
        else RestoreCursorBackgroundText();
        mouse.hidden = 1;

        Mouse_NewVideoMode();
        Mouse_SetMickeyPixelRate(8,16);

        mouse.mickey_x = 0;
        mouse.mickey_y = 0;

        // Dont set max coordinates here. it is done by SetResolution!
        mouse.x = (float)((mouse.max_x + 1)/ 2);
        mouse.y = (float)((mouse.max_y + 1)/ 2);
        mouse.sub_mask = 0;
        mouse.in_UIR = false;
    }

    static private Callback.Handler INT33_Handler = new Callback.Handler() {
        public String getName() {
            return "Mouse.INT33_Handler";
        }
        public /*Bitu*/int call() {
        //	Log.log(LogTypes.LOG_MOUSE,LogSeverities.LOG_NORMAL,"MOUSE: %04X %X %X %d %d",reg_ax,reg_bx,reg_cx,POS_X,POS_Y);
            switch (CPU_Regs.reg_eax.word()) {
            case 0x00:	/* Reset Driver and Read Status */
                Mouse_ResetHardware(); /* fallthrough */
            case 0x21:	/* Software Reset */
                CPU_Regs.reg_eax.word(0xffff);
                CPU_Regs.reg_ebx.word(MOUSE_BUTTONS);
                Mouse_Reset();
                Main.Mouse_AutoLock(true);
                Keyboard.AUX_INT33_Takeover();
                break;
            case 0x01:	/* Show Mouse */
                if(mouse.hidden!=0) mouse.hidden--;
                Main.Mouse_AutoLock(true);
                DrawCursor();
                break;
            case 0x02:	/* Hide Mouse */
                {
                    if (Int10_modes.CurMode.type!=VGA.M_TEXT) RestoreCursorBackground();
                    else RestoreCursorBackgroundText();
                    mouse.hidden++;
                }
                break;
            case 0x03:	/* Return position and Button Status */
                CPU_Regs.reg_ebx.word(mouse.buttons);
                CPU_Regs.reg_ecx.word(POS_X());
                CPU_Regs.reg_edx.word(POS_Y());
                break;
            case 0x04:	/* Position Mouse */
                /* If position isn't different from current position
                 * don't change it then. (as position is rounded so numbers get
                 * lost when the rounded number is set) (arena/simulation Wolf) */
                if ((/*Bit16s*/short)CPU_Regs.reg_ecx.word() >= mouse.max_x) mouse.x = (float)(mouse.max_x);
                else if (mouse.min_x >= CPU_Regs.reg_ecx.word()) mouse.x = (float)(mouse.min_x);
                else if (CPU_Regs.reg_ecx.word() != POS_X()) mouse.x = (float)(CPU_Regs.reg_ecx.word());

                if ((/*Bit16s*/short)CPU_Regs.reg_edx.word() >= mouse.max_y) mouse.y = (float)(mouse.max_y);
                else if (mouse.min_y >= (/*Bit16s*/short)CPU_Regs.reg_edx.word()) mouse.y = (float)(mouse.min_y);
                else if ((/*Bit16s*/short)CPU_Regs.reg_edx.word() != POS_Y()) mouse.y = (float)(CPU_Regs.reg_edx.word());
                DrawCursor();
                break;
            case 0x05:	/* Return Button Press Data */
                {
                    /*Bit16u*/int but=CPU_Regs.reg_ebx.word();
                    CPU_Regs.reg_eax.word(mouse.buttons);
                    if (but>=MOUSE_BUTTONS) but = MOUSE_BUTTONS - 1;
                    CPU_Regs.reg_ecx.word(mouse.last_pressed_x[but]);
                    CPU_Regs.reg_edx.word(mouse.last_pressed_y[but]);
                    CPU_Regs.reg_ebx.word(mouse.times_pressed[but]);
                    mouse.times_pressed[but]=0;
                    break;
                }
            case 0x06:	/* Return Button Release Data */
                {
                    /*Bit16u*/int but=CPU_Regs.reg_ebx.word();
                    CPU_Regs.reg_eax.word(mouse.buttons);
                    if (but>=MOUSE_BUTTONS) but = MOUSE_BUTTONS - 1;
                    CPU_Regs.reg_ecx.word(mouse.last_released_x[but]);
                    CPU_Regs.reg_edx.word(mouse.last_released_y[but]);
                    CPU_Regs.reg_ebx.word(mouse.times_released[but]);
                    mouse.times_released[but]=0;
                    break;
                }
            case 0x07:	/* Define horizontal cursor range */
                {	//lemmings set 1-640 and wants that. iron seeds set 0-640 but doesn't like 640
                    //Iron seed works if newvideo mode with mode 13 sets 0-639
                    //Larry 6 actually wants newvideo mode with mode 13 to set it to 0-319
                    /*Bit16s*/short max,min;
                    if ((/*Bit16s*/short)CPU_Regs.reg_ecx.word()<(/*Bit16s*/short)CPU_Regs.reg_edx.word()) { min=(/*Bit16s*/short)CPU_Regs.reg_ecx.word();max=(/*Bit16s*/short)CPU_Regs.reg_edx.word();}
                    else { min=(/*Bit16s*/short)CPU_Regs.reg_edx.word();max=(/*Bit16s*/short)CPU_Regs.reg_ecx.word();}
                    mouse.min_x=min;
                    mouse.max_x=max;
                    /* Battlechess wants this */
                    if(mouse.x > mouse.max_x) mouse.x = mouse.max_x;
                    if(mouse.x < mouse.min_x) mouse.x = mouse.min_x;
                    /* Or alternatively this:
                    mouse.x = (mouse.max_x - mouse.min_x + 1)/2;*/
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_MOUSE,LogSeverities.LOG_NORMAL,"Define Hortizontal range min:"+min+" max:"+max);
                }
                break;
            case 0x08:	/* Define vertical cursor range */
                {	// not sure what to take instead of the Int10_modes.CurMode (see case 0x07 as well)
                    // especially the cases where sheight= 400 and we set it with the mouse_reset to 200
                    //disabled it at the moment. Seems to break syndicate who want 400 in mode 13
                    /*Bit16s*/short max,min;
                    if ((/*Bit16s*/short)CPU_Regs.reg_ecx.word()<(/*Bit16s*/short)CPU_Regs.reg_edx.word()) { min=(/*Bit16s*/short)CPU_Regs.reg_ecx.word();max=(/*Bit16s*/short)CPU_Regs.reg_edx.word();}
                    else { min=(/*Bit16s*/short)CPU_Regs.reg_edx.word();max=(/*Bit16s*/short)CPU_Regs.reg_ecx.word();}
                    mouse.min_y=min;
                    mouse.max_y=max;
                    /* Battlechess wants this */
                    if(mouse.y > mouse.max_y) mouse.y = mouse.max_y;
                    if(mouse.y < mouse.min_y) mouse.y = mouse.min_y;
                    /* Or alternatively this:
                    mouse.y = (mouse.max_y - mouse.min_y + 1)/2;*/
                    if (Log.level<=LogSeverities.LOG_NORMAL) Log.log(LogTypes.LOG_MOUSE,LogSeverities.LOG_NORMAL,"Define Vertical range min:"+min+" max:"+max);
                }
                break;
            case 0x09:	/* Define GFX Cursor */
                {
                    /*PhysPt*/int src = CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edx.word();
                    Memory.MEM_BlockRead16u(src          ,userdefScreenMask,0,CURSORY);
                    Memory.MEM_BlockRead16u(src+CURSORY*2,userdefCursorMask,0,CURSORY);
                    mouse.screenMask = userdefScreenMask;
                    mouse.cursorMask = userdefCursorMask;
                    mouse.hotx		 = (short)CPU_Regs.reg_ebx.word();
                    mouse.hoty		 = (short)CPU_Regs.reg_ecx.word();
                    mouse.cursorType = 2;
                    DrawCursor();
                }
                break;
            case 0x0a:	/* Define Text Cursor */
                mouse.cursorType = CPU_Regs.reg_ebx.word();
                mouse.textAndMask = CPU_Regs.reg_ecx.word();
                mouse.textXorMask = CPU_Regs.reg_edx.word();
                break;
            case 0x0b:	/* Read Motion Data */
                CPU_Regs.reg_ecx.word((short)(mouse.mickey_x));
                CPU_Regs.reg_edx.word((short)(mouse.mickey_y));
                mouse.mickey_x=0;
                mouse.mickey_y=0;
                break;
            case 0x0c:	/* Define interrupt subroutine parameters */
                mouse.sub_mask=CPU_Regs.reg_ecx.word();
                mouse.sub_seg=(int)CPU_Regs.reg_esVal.dword;
                mouse.sub_ofs=CPU_Regs.reg_edx.word();
                Main.Mouse_AutoLock(true); //Some games don't seem to reset the mouse before using
                break;
            case 0x0f:	/* Define mickey/pixel rate */
                Mouse_SetMickeyPixelRate(CPU_Regs.reg_ecx.word(),CPU_Regs.reg_edx.word());
                break;
            case 0x10:      /* Define screen region for updating */
                mouse.updateRegion_x[0]=CPU_Regs.reg_ecx.word();
                mouse.updateRegion_y[0]=CPU_Regs.reg_edx.word();
                mouse.updateRegion_x[1]=CPU_Regs.reg_esi.word();
                mouse.updateRegion_y[1]=CPU_Regs.reg_edi.word();
                break;
            case 0x11:      /* Get number of buttons */
                CPU_Regs.reg_eax.word(0xffff);
                CPU_Regs.reg_ebx.word(MOUSE_BUTTONS);
                break;
            case 0x13:      /* Set double-speed threshold */
                mouse.doubleSpeedThreshold=(CPU_Regs.reg_ebx.word()!=0 ? CPU_Regs.reg_ebx.word() : 64);
                break;
            case 0x14: /* Exchange event-handler */
                {
                    /*Bit16u*/int oldSeg = mouse.sub_seg;
                    /*Bit16u*/int oldOfs = mouse.sub_ofs;
                    /*Bit16u*/int oldMask= mouse.sub_mask;
                    // Set new values
                    mouse.sub_mask= CPU_Regs.reg_ecx.word();
                    mouse.sub_seg = (int)CPU_Regs.reg_esVal.dword;
                    mouse.sub_ofs = CPU_Regs.reg_edx.word();
                    // Return old values
                    CPU_Regs.reg_ecx.word(oldMask);
                    CPU_Regs.reg_edx.word(oldOfs);
                    CPU_Regs.SegSet16ES(oldSeg);
                }
                break;
            case 0x15: /* Get Driver storage space requirements */
                CPU_Regs.reg_ebx.word(_mouse.sizeof());
                break;
            case 0x16: /* Save driver state */
                {
                    Log.log(LogTypes.LOG_MOUSE,LogSeverities.LOG_WARN,"Saving driver state...");
                    /*PhysPt*/int dest = CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edx.word();
                    byte[] data = mouse.save();
                    Memory.MEM_BlockWrite(dest, data, data.length);
                }
                break;
            case 0x17: /* load driver state */
                {
                    Log.log(LogTypes.LOG_MOUSE,LogSeverities.LOG_WARN,"Loading driver state...");
                    /*PhysPt*/int src = CPU_Regs.reg_esPhys.dword+CPU_Regs.reg_edx.word();
                    byte[] data = new byte[_mouse.sizeof()];
                    Memory.MEM_BlockRead(src, data, data.length);
                    mouse.load(data);
                }
                break;
            case 0x1a:	/* Set mouse sensitivity */
                // ToDo : double mouse speed value
                Mouse_SetSensitivity(CPU_Regs.reg_ebx.word(),CPU_Regs.reg_ecx.word(),CPU_Regs.reg_edx.word());

                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_MOUSE,LogSeverities.LOG_WARN,"Set sensitivity used with "+CPU_Regs.reg_ebx.word()+" "+CPU_Regs.reg_ecx.word()+" ("+CPU_Regs.reg_edx.word()+")");
                break;
            case 0x1b:	/* Get mouse sensitivity */
                CPU_Regs.reg_ebx.word(mouse.senv_x_val);
                CPU_Regs.reg_ecx.word(mouse.senv_y_val);
                CPU_Regs.reg_edx.word(mouse.dspeed_val);

                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_MOUSE,LogSeverities.LOG_WARN,"Get sensitivity "+CPU_Regs.reg_ebx.word()+" "+CPU_Regs.reg_ecx.word());
                break;
            case 0x1c:	/* Set interrupt rate */
                /* Can't really set a rate this is host determined */
                break;
            case 0x1d:      /* Set display page number */
                mouse.page=(short)CPU_Regs.reg_ebx.low();
                break;
            case 0x1e:      /* Get display page number */
                CPU_Regs.reg_ebx.word(mouse.page);
                break;
            case 0x1f:	/* Disable Mousedriver */
                /* ES:BX old mouse driver Zero at the moment TODO */
                CPU_Regs.reg_ebx.word(0);
                CPU_Regs.SegSet16ES(0);
                mouse.enabled=false; /* Just for reporting not doing a thing with it */
                mouse.oldhidden=mouse.hidden;
                mouse.hidden=1;
                break;
            case 0x20:	/* Enable Mousedriver */
                mouse.enabled=true;
                mouse.hidden=mouse.oldhidden;
                break;
            case 0x22:      /* Set language for messages */
                    /*
                     *                        Values for mouse driver language:
                     *
                     *                        00h     English
                     *                        01h     French
                     *                        02h     Dutch
                     *                        03h     German
                     *                        04h     Swedish
                     *                        05h     Finnish
                     *                        06h     Spanish
                     *                        07h     Portugese
                     *                        08h     Italian
                     *
                     */
                mouse.language=CPU_Regs.reg_ebx.word();
                break;
            case 0x23:      /* Get language for messages */
                CPU_Regs.reg_ebx.word(mouse.language);
                break;
            case 0x24:	/* Get Software version and mouse type */
                CPU_Regs.reg_ebx.word(0x805);	//Version 8.05 woohoo
                CPU_Regs.reg_ecx.high(0x04);	/* PS/2 type */
                CPU_Regs.reg_ecx.low(0);		/* PS/2 (unused) */
                break;
            case 0x26: /* Get Maximum virtual coordinates */
                CPU_Regs.reg_ebx.word((mouse.enabled ? 0x0000 : 0xffff));
                CPU_Regs.reg_ecx.word((/*Bit16u*/int)mouse.max_x);
                CPU_Regs.reg_edx.word((/*Bit16u*/int)mouse.max_y);
                break;
            case 0x31: /* Get Current Minimum/Maximum virtual coordinates */
                CPU_Regs.reg_eax.word((/*Bit16u*/int)mouse.min_x);
                CPU_Regs.reg_ebx.word((/*Bit16u*/int)mouse.min_y);
                CPU_Regs.reg_ecx.word((/*Bit16u*/int)mouse.max_x);
                CPU_Regs.reg_edx.word((/*Bit16u*/int)mouse.max_y);
                break;
            default:
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_MOUSE,LogSeverities.LOG_ERROR,"Mouse Function "+Integer.toString(CPU_Regs.reg_eax.word(), 16)+" not implemented!");
                break;
            }
            return Callback.CBRET_NONE;
        }
    };

    static private Callback.Handler MOUSE_BD_Handler = new Callback.Handler() {
        public String getName() {
            return "Mouse.MOUSE_BD_Handler";
        }
        public /*Bitu*/int call() {
            // the stack contains offsets to register values
            /*Bit16u*/int raxpt=Memory.real_readw((int)CPU_Regs.reg_ssVal.dword,CPU_Regs.reg_esp.word()+0x0a);
            /*Bit16u*/int rbxpt=Memory.real_readw((int)CPU_Regs.reg_ssVal.dword,CPU_Regs.reg_esp.word()+0x08);
            /*Bit16u*/int rcxpt=Memory.real_readw((int)CPU_Regs.reg_ssVal.dword,CPU_Regs.reg_esp.word()+0x06);
            /*Bit16u*/int rdxpt=Memory.real_readw((int)CPU_Regs.reg_ssVal.dword,CPU_Regs.reg_esp.word()+0x04);

            // read out the actual values, registers ARE overwritten
            /*Bit16u*/int rax=Memory.real_readw((int)CPU_Regs.reg_dsVal.dword,raxpt);
            CPU_Regs.reg_eax.word(rax);
            CPU_Regs.reg_ebx.word(Memory.real_readw((int)CPU_Regs.reg_dsVal.dword,rbxpt));
            CPU_Regs.reg_ecx.word(Memory.real_readw((int)CPU_Regs.reg_dsVal.dword,rcxpt));
            CPU_Regs.reg_edx.word(Memory.real_readw((int)CPU_Regs.reg_dsVal.dword,rdxpt));
        //	LOG_MSG("MOUSE BD: %04X %X %X %X %d %d",CPU_Regs.reg_eax.word(),CPU_Regs.reg_ebx.word(),CPU_Regs.reg_ecx.word(),CPU_Regs.reg_edx.word(),POS_X,POS_Y);

            // some functions are treated in a special way (additional registers)
            switch (rax) {
                case 0x09:	/* Define GFX Cursor */
                case 0x16:	/* Save driver state */
                case 0x17:	/* load driver state */
                    CPU_Regs.SegSet16ES((int)CPU_Regs.reg_dsVal.dword);
                    break;
                case 0x0c:	/* Define interrupt subroutine parameters */
                case 0x14:	/* Exchange event-handler */
                    if (CPU_Regs.reg_ebx.word()!=0) CPU_Regs.SegSet16ES(CPU_Regs.reg_ebx.word());
                    else CPU_Regs.SegSet16ES((int)CPU_Regs.reg_dsVal.dword);
                    break;
                case 0x10:	/* Define screen region for updating */
                    CPU_Regs.reg_ecx.word(Memory.real_readw((int)CPU_Regs.reg_dsVal.dword,rdxpt));
                    CPU_Regs.reg_edx.word(Memory.real_readw((int)CPU_Regs.reg_dsVal.dword,rdxpt+2));
                    CPU_Regs.reg_esi.word(Memory.real_readw((int)CPU_Regs.reg_dsVal.dword,rdxpt+4));
                    CPU_Regs.reg_edi.word(Memory.real_readw((int)CPU_Regs.reg_dsVal.dword,rdxpt+6));
                    break;
                default:
                    break;
            }

            INT33_Handler.call();

            // save back the registers, too
            Memory.real_writew((int)CPU_Regs.reg_dsVal.dword,raxpt,CPU_Regs.reg_eax.word());
            Memory.real_writew((int)CPU_Regs.reg_dsVal.dword,rbxpt,CPU_Regs.reg_ebx.word());
            Memory.real_writew((int)CPU_Regs.reg_dsVal.dword,rcxpt,CPU_Regs.reg_ecx.word());
            Memory.real_writew((int)CPU_Regs.reg_dsVal.dword,rdxpt,CPU_Regs.reg_edx.word());
            switch (rax) {
                case 0x1f:	/* Disable Mousedriver */
                    Memory.real_writew((int)CPU_Regs.reg_dsVal.dword,rbxpt,(int)CPU_Regs.reg_esVal.dword);
                    break;
                case 0x14: /* Exchange event-handler */
                    Memory.real_writew((int)CPU_Regs.reg_dsVal.dword,rcxpt,(int)CPU_Regs.reg_esVal.dword);
                    break;
                default:
                    break;
            }

            CPU_Regs.reg_eax.word(rax);
            return Callback.CBRET_NONE;
        }
    };

    static private Callback.Handler INT74_Handler = new Callback.Handler() {
        public String getName() {
            return "Mouse.INT74_Handler";
        }
        public /*Bitu*/int call() {
            if (mouse.events>0) {
                mouse.events--;
                /* Check for an active Interrupt Handler that will get called */
                if ((mouse.sub_mask & mouse.event_queue[mouse.events].type)!=0) {
                    CPU_Regs.reg_eax.word(mouse.event_queue[mouse.events].type);
                    CPU_Regs.reg_ebx.word(mouse.event_queue[mouse.events].buttons);
                    CPU_Regs.reg_ecx.word(POS_X());
                    CPU_Regs.reg_edx.word(POS_Y());
                    CPU_Regs.reg_esi.word((/*Bit16s*/short)(mouse.mickey_x));
                    CPU_Regs.reg_edi.word((/*Bit16s*/short)(mouse.mickey_y));
                    CPU.CPU_Push16(Memory.RealSeg(Callback.CALLBACK_RealPointer(int74_ret_callback)));
                    CPU.CPU_Push16(Memory.RealOff(Callback.CALLBACK_RealPointer(int74_ret_callback)));
                    CPU_Regs.SegSet16CS(mouse.sub_seg);
                    CPU_Regs.reg_ip(mouse.sub_ofs);
                    if(mouse.in_UIR) Log.log(LogTypes.LOG_MOUSE,LogSeverities.LOG_ERROR,"Already in UIR!");
                    mouse.in_UIR = true;
                } else if (useps2callback) {
                    CPU.CPU_Push16(Memory.RealSeg(Callback.CALLBACK_RealPointer(int74_ret_callback)));
                    CPU.CPU_Push16(Memory.RealOff(Callback.CALLBACK_RealPointer(int74_ret_callback)));
                    DoPS2Callback(mouse.event_queue[mouse.events].buttons, (short)mouse.x, (short)mouse.y);
                } else {
                    CPU_Regs.SegSet16CS(Memory.RealSeg(Callback.CALLBACK_RealPointer(int74_ret_callback)));
                    CPU_Regs.reg_ip(Memory.RealOff(Callback.CALLBACK_RealPointer(int74_ret_callback)));
                }
            } else {
                CPU_Regs.SegSet16CS(Memory.RealSeg(Callback.CALLBACK_RealPointer(int74_ret_callback)));
                CPU_Regs.reg_ip(Memory.RealOff(Callback.CALLBACK_RealPointer(int74_ret_callback)));
            }
            return Callback.CBRET_NONE;
        }
    };

    static private Callback.Handler MOUSE_UserInt_CB_Handler = new Callback.Handler() {
        public String getName() {
            return "Mouse.MOUSE_UserInt_CB_Handler";
        }
        public /*Bitu*/int call() {
            mouse.in_UIR = false;
            if (mouse.events!=0) {
                if (!mouse.timer_in_progress) {
                    mouse.timer_in_progress = true;
                    Pic.PIC_AddEvent(MOUSE_Limit_Events,MOUSE_DELAY);
                }
            }
            return Callback.CBRET_NONE;
        }
    };

    public static Section.SectionFunction MOUSE_Destroy = new Section.SectionFunction() {
        public void call(Section section) {
            mouse = new _mouse();
        }
    };

    public static Section.SectionFunction MOUSE_Init = new Section.SectionFunction() {
        public void call(Section section) {
            // Callback for mouse interrupt 0x33
            call_int33=Callback.CALLBACK_Allocate();
        //	/*RealPt*/int i33loc=RealMake(CB_SEG+1,(call_int33*CB_SIZE)-0x10);
            /*RealPt*/int i33loc=Memory.RealMake(Dos_tables.DOS_GetMemory(0x1)-1,0x10);
            Callback.CALLBACK_Setup(call_int33,INT33_Handler,Callback.CB_MOUSE,Memory.Real2Phys(i33loc),"Mouse");
            // Wasteland needs low(seg(int33))!=0 and low(ofs(int33))!=0
            Memory.real_writed(0,0x33<<2,i33loc);

            call_mouse_bd=Callback.CALLBACK_Allocate();
            Callback.CALLBACK_Setup(call_mouse_bd,MOUSE_BD_Handler,Callback.CB_RETF8,
                Memory.PhysMake(Memory.RealSeg(i33loc),Memory.RealOff(i33loc)+2),"MouseBD");
            // pseudocode for CB_MOUSE (including the special backdoor entry point):
            //	jump near i33hd
            //	callback MOUSE_BD_Handler
            //	retf 8
            //  label i33hd:
            //	callback INT33_Handler
            //	iret


            // Callback for ps2 irq
            call_int74=Callback.CALLBACK_Allocate();
            Callback.CALLBACK_Setup(call_int74,INT74_Handler,Callback.CB_IRQ12,"int 74");
            // pseudocode for CB_IRQ12:
            //	push ds
            //	push es
            //	pushad
            //	sti
            //	callback INT74_Handler
            //		doesn't return here, but rather to CB_IRQ12_RET
            //		(ps2 callback/user callback inbetween if requested)

            int74_ret_callback=Callback.CALLBACK_Allocate();
            Callback.CALLBACK_Setup(int74_ret_callback,MOUSE_UserInt_CB_Handler,Callback.CB_IRQ12_RET,"int 74 ret");
            // pseudocode for CB_IRQ12_RET:
            //	callback MOUSE_UserInt_CB_Handler
            //	cli
            //	mov al, 0x20
            //	out 0xa0, al
            //	out 0x20, al
            //	popad
            //	pop es
            //	pop ds
            //	iret

            /*Bit8u*/short hwvec=(MOUSE_IRQ>7)?(0x70+MOUSE_IRQ-8):(0x8+MOUSE_IRQ);
            Memory.RealSetVec(hwvec,Callback.CALLBACK_RealPointer(call_int74));

            // Callback for ps2 user callback handling
            useps2callback = false; ps2callbackinit = false;
            call_ps2=Callback.CALLBACK_Allocate();
            Callback.CALLBACK_Setup(call_ps2,PS2_Handler,Callback.CB_RETF,"ps2 bios callback");
            ps2_callback=Callback.CALLBACK_RealPointer(call_ps2);

            mouse.hidden = 1; //Hide mouse on startup
            mouse.timer_in_progress = false;
            mouse.mode = 0xFF; //Non existing mode

            mouse.sub_mask=0;
            mouse.sub_seg=0x6362;	// magic value
            mouse.sub_ofs=0;

            Mouse_ResetHardware();
            Mouse_Reset();
            Mouse_SetSensitivity(50,50,50);

            section.AddDestroyFunction(MOUSE_Destroy,false);
        }
    };
}
