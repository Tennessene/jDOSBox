package jdos.hardware;

import jdos.Dosbox;
import jdos.dos.Dos_programs;
import jdos.gui.Main;
import jdos.misc.Log;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.types.MachineType;

public class Keyboard {
    public static final class KBD_KEYS {
        static public final int KBD_NONE=0;
        static public final int KBD_1=1;
        static public final int KBD_2=2;
        static public final int KBD_3=3;
        static public final int KBD_4=4;
        static public final int KBD_5=5;
        static public final int KBD_6=6;
        static public final int KBD_7=7;
        static public final int KBD_8=8;
        static public final int KBD_9=9;
        static public final int KBD_0=10;
        static public final int KBD_q=11;
        static public final int KBD_w=12;
        static public final int KBD_e=13;
        static public final int KBD_r=14;
        static public final int KBD_t=15;
        static public final int KBD_y=16;
        static public final int KBD_u=17;
        static public final int KBD_i=18;
        static public final int KBD_o=19;
        static public final int KBD_p=20;
        static public final int KBD_a=21;
        static public final int KBD_s=22;
        static public final int KBD_d=23;
        static public final int KBD_f=24;
        static public final int KBD_g=25;
        static public final int KBD_h=26;
        static public final int KBD_j=27;
        static public final int KBD_k=28;
        static public final int KBD_l=29;
        static public final int KBD_z=30;
        static public final int KBD_x=31;
        static public final int KBD_c=32;
        static public final int KBD_v=33;
        static public final int KBD_b=34;
        static public final int KBD_n=35;
        static public final int KBD_m=36;
        static public final int KBD_f1=37;
        static public final int KBD_f2=38;
        static public final int KBD_f3=39;
        static public final int KBD_f4=40;
        static public final int KBD_f5=41;
        static public final int KBD_f6=42;
        static public final int KBD_f7=43;
        static public final int KBD_f8=44;
        static public final int KBD_f9=45;
        static public final int KBD_f10=46;
        static public final int KBD_f11=47;
        static public final int KBD_f12=48;

        /*Now the weirder keys */

        static public final int KBD_esc=49;
        static public final int KBD_tab=50;
        static public final int KBD_backspace=51;
        static public final int KBD_enter=52;
        static public final int KBD_space=53;
        static public final int KBD_leftalt=54;
        static public final int KBD_rightalt=55;
        static public final int KBD_leftctrl=56;
        static public final int KBD_rightctrl=57;
        static public final int KBD_leftshift=58;
        static public final int KBD_rightshift=59;
        static public final int KBD_capslock=60;
        static public final int KBD_scrolllock=61;
        static public final int KBD_numlock=62;

        static public final int KBD_grave=63;
        static public final int KBD_minus=64;
        static public final int KBD_equals=65;
        static public final int KBD_backslash=66;
        static public final int KBD_leftbracket=67;
        static public final int KBD_rightbracket=68;
        static public final int KBD_semicolon=69;
        static public final int KBD_quote=70;
        static public final int KBD_period=71;
        static public final int KBD_comma=72;
        static public final int KBD_slash=73;
        static public final int KBD_extra_lt_gt=74;

        static public final int KBD_printscreen=75;
        static public final int KBD_pause=76;
        static public final int KBD_insert=77;
        static public final int KBD_home=78;
        static public final int KBD_pageup=79;
        static public final int KBD_delete=80;
        static public final int KBD_end=81;
        static public final int KBD_pagedown=82;
        static public final int KBD_left=83;
        static public final int KBD_up=84;
        static public final int KBD_down=85;
        static public final int KBD_right=86;

        static public final int KBD_kp1=87;
        static public final int KBD_kp2=88;
        static public final int KBD_kp3=89;
        static public final int KBD_kp4=90;
        static public final int KBD_kp5=91;
        static public final int KBD_kp6=92;
        static public final int KBD_kp7=93;
        static public final int KBD_kp8=94;
        static public final int KBD_kp9=95;
        static public final int KBD_kp0=96;
        static public final int KBD_kpdivide=97;
        static public final int KBD_kpmultiply=98;
        static public final int KBD_kpminus=99;
        static public final int KBD_kpplus=100;
        static public final int KBD_kpenter=101;
        static public final int KBD_kpperiod=102;


        static public final int KBD_LAST=103;
    }
    static private final int KEYBUFSIZE = 32;
    static private final float KEYDELAY = 0.300f;			//Considering 20-30 khz serial clock and 11 bits/char

    static final private class KeyCommands {
        static public final int CMD_NONE=0;
        static public final int CMD_SETLEDS=1;
        static public final int CMD_SETTYPERATE=2;
        static public final int CMD_SETOUTPORT=3;
        static public final int CMD_SETCOMMAND=4;
        static public final int CMD_WRITEOUTPUT=5;
        static public final int CMD_WRITEAUXOUT=6;
        static public final int CMD_SETSCANSET=7;
        static public final int CMD_WRITEAUX=8;
    }

    static public final int ACMD_NONE=0;
	static public final int ACMD_SET_RATE=1;
	static public final int ACMD_SET_RESOLUTION=2;
    
    static private final int AUX = 0x100;
    static private final int RESETDELAY = 100;

    /* Status Register Bits */
    private static final byte KBD_STAT_OBF = (byte)0x01; /* Keyboard output buffer full */
    private static final byte KBD_STAT_IBF = (byte)0x02; /* Keyboard input buffer full */
    private static final byte KBD_STAT_SELFTEST = (byte)0x04; /* Self test successful */
    private static final byte KBD_STAT_CMD = (byte)0x08; /* Last write was a command write (0=data) */
    private static final byte KBD_STAT_UNLOCKED = (byte)0x10; /* Zero if keyboard locked */
    private static final byte KBD_STAT_MOUSE_OBF = (byte)0x20; /* Mouse output buffer full */
    private static final byte KBD_STAT_GTO = (byte)0x40; /* General receive/xmit timeout */
    private static final byte KBD_STAT_PERR = (byte)0x80; /* Parity error */

    private static final byte MM_REMOTE=0;
    private static final byte MM_WRAP=1;
    private static final byte MM_STREAM=2;

    private static final byte MOUSE_NONE=0;
    private static final byte MOUSE_2BUTTON=1;
    private static final byte MOUSE_3BUTTON=2;
    private static final byte MOUSE_INTELLIMOUSE=3;
    private static final byte MOUSE_INTELLIMOUSE45=4;

    private static class ps2mouse {
        int	type;			/* what kind of mouse we are emulating */
        int	mode;			/* current mode */
        int		samplerate;		/* current sample rate */
        int		resolution;		/* current resolution */
        int[]		last_srate=new int[3];		/* last 3 "set sample rate" values */
        float		acx,acy;		/* accumulator */
        boolean		reporting;		/* reporting */
        boolean		scale21;		/* 2:1 scaling */
        boolean		intellimouse_mode;	/* intellimouse scroll wheel */
        boolean		intellimouse_btn45;	/* 4th & 5th buttons */
        boolean		int33_taken;		/* for compatability with existing DOSBox code: allow INT 33H emulation to "take over" and disable us */
        boolean		l,m,r;			/* mouse button states */
    }

    private static class Keyb {
        /*Bit8u*/int[] buf8042 = new int[8];		/* for 8042 responses, taking priority over keyboard responses */
        /*Bitu*/int buf8042_len;
        /*Bitu*/int buf8042_pos;

        /*Bit8u*/int[] buffer=new int[KEYBUFSIZE];
        /*Bitu*/int used;
        /*Bitu*/int pos;
        static public class Repeat {
            int key;
            /*Bitu*/int wait;
            /*Bitu*/int pause,rate;
        }
        Repeat repeat = new Repeat();
        int command;
        int aux_command;

        ps2mouse ps2mouse = new ps2mouse();
        int scanset;
        boolean enable_aux;
        boolean reset;
        boolean auxchanged;
        boolean auxactive;
        boolean cb_irq12;			/* PS/2 mouse */
        boolean cb_irq1;
        boolean cb_xlat;
        boolean cb_sys;

        /*Bit8u*/short p60data;
        boolean p60changed;
        boolean active;
        boolean scanning;
        boolean scheduled;
        int status = KBD_STAT_CMD | KBD_STAT_UNLOCKED;
    }

    private static Keyb keyb = new Keyb();

    /* NTS: INT33H emulation is coded to call this ONLY if it hasn't taken over the role of mouse input */
    static public void KEYBOARD_AUX_Event(float x1,float y1,int buttons) {
        keyb.ps2mouse.acx += x1;
        keyb.ps2mouse.acy += y1;
        keyb.ps2mouse.l = (buttons & 1)>0;
        keyb.ps2mouse.r = (buttons & 2)>0;
        keyb.ps2mouse.m = (buttons & 4)>0;

        if (keyb.ps2mouse.reporting && keyb.ps2mouse.mode == MM_STREAM) {
            if ((keyb.used+4) < KEYBUFSIZE) {
                int x,y;

                //x = (int)(keyb.ps2mouse.acx * (1 << keyb.ps2mouse.resolution));
                x = (int)keyb.ps2mouse.acx;
                if (x < -256) x = -256;
                else if (x > 255) x = 255;

                // y = -((int)(keyb.ps2mouse.acy * (1 << keyb.ps2mouse.resolution)));
                y = -(int)keyb.ps2mouse.acy;
                if (y < -256) y = -256;
                else if (y > 255) y = 255;

                KEYBOARD_AddBuffer(AUX|
                    ((y == -256 || y == 255) ? 0x80 : 0x00) |	/* Y overflow */
                    ((x == -256 || x == 255) ? 0x40 : 0x00) |	/* X overflow */
                    ((y & 0x100)!=0 ? 0x20 : 0x00) |			/* Y sign bit */
                    ((x & 0x100)!=0 ? 0x10 : 0x00) |			/* X sign bit */
                    0x08 |						/* always 1? */
                    (keyb.ps2mouse.m ? 4 : 0) |			/* M */
                    (keyb.ps2mouse.r ? 2 : 0) |			/* R */
                    (keyb.ps2mouse.l ? 1 : 0));			/* L */
                KEYBOARD_AddBuffer(AUX|(x&0xFF));
                KEYBOARD_AddBuffer(AUX|(y&0xFF));
                if (keyb.ps2mouse.intellimouse_btn45) {
                    KEYBOARD_AddBuffer(AUX|0x00);			/* TODO: scrollwheel and 4th & 5th buttons */
                }
                else if (keyb.ps2mouse.intellimouse_mode) {
                    KEYBOARD_AddBuffer(AUX|0x00);			/* TODO: scrollwheel */
                }
            }

            keyb.ps2mouse.acx = 0;
            keyb.ps2mouse.acy = 0;
        }
    }

    static public boolean KEYBOARD_AUX_Active() {
        /* NTS: We want to allow software to read by polling, which doesn't
         *      require interrupts to be enabled. Whether or not IRQ12 is
         *      unmasked is irrelevent */
        return keyb.auxactive && !keyb.ps2mouse.int33_taken;
    }

    private static void KEYBOARD_SetPort60(/*Bit8u*/int val) {
        keyb.auxchanged=(val&AUX)>0;
        keyb.p60changed=true;
        keyb.p60data=(byte)(val & 0xFF);
        if (keyb.auxchanged) {
            if (keyb.cb_irq12) {
                Pic.PIC_ActivateIRQ(12);
            }
        } else if (keyb.cb_irq1) {
            if (Dosbox.machine== MachineType.MCH_PCJR) Pic.PIC_ActivateIRQ(6);
            else Pic.PIC_ActivateIRQ(1);
        }
    }

    private static void updateIRQ() {
        if (keyb.p60changed || keyb.buf8042_len>0) {
            if (Dosbox.machine== MachineType.MCH_PCJR) Pic.PIC_ActivateIRQ(6);
            else Pic.PIC_ActivateIRQ(1);
        } else {
            if (Dosbox.machine== MachineType.MCH_PCJR) Pic.PIC_DeActivateIRQ(6);
            else Pic.PIC_DeActivateIRQ(1);
        }
    }

    private static Pic.PIC_EventHandler KEYBOARD_ResetDelay = new Pic.PIC_EventHandler() {
        public void call(/*Bitu*/int val) {
            keyb.reset=false;
            KEYBOARD_SetLEDs(0);
            KEYBOARD_Add8042Response(0xAA);
        }
    };

    private static Pic.PIC_EventHandler KEYBOARD_TransferBuffer = new Pic.PIC_EventHandler() {
        public void call(/*Bitu*/int val) {
            /* 8042 responses take priority over the keyboard */
            if (keyb.buf8042_len != 0) {
                KEYBOARD_SetPort60(keyb.buf8042[keyb.buf8042_pos]);
                if (++keyb.buf8042_pos >= keyb.buf8042_len)
                    keyb.buf8042_len = keyb.buf8042_pos = 0;
                return;
            }
            keyb.scheduled=false;
            if (keyb.used==0) {
                Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"Transfer started with empty buffer");
                return;
            }
            KEYBOARD_SetPort60(keyb.buffer[keyb.pos]);
            if (++keyb.pos>=KEYBUFSIZE) keyb.pos-=KEYBUFSIZE;
            keyb.used--;
        }
        public String toString() {
            return "KEYBOARD_TransferBuffer";
        }
    };


    public static void KEYBOARD_ClrBuffer() {
        keyb.buf8042_len=0;
	    keyb.buf8042_pos=0;
        keyb.used=0;
        keyb.pos=0;
        Pic.PIC_RemoveEvents(KEYBOARD_TransferBuffer);
        keyb.scheduled=false;
    }

    static void KEYBOARD_Add8042Response(int data) {
        if (keyb.buf8042_pos >= keyb.buf8042_len)
            keyb.buf8042_pos = keyb.buf8042_len = 0;
        else if (keyb.buf8042_len == 0)
            keyb.buf8042_pos = 0;

        if (keyb.buf8042_pos >= keyb.buf8042.length) {
            Log.log_msg("8042 Buffer full, dropping code");
            return;
        }

        keyb.buf8042[keyb.buf8042_len++] = data;
        if ((data & AUX) != 0)
            keyb.auxchanged = true;
        else
            keyb.p60changed = true;

        if (keyb.auxchanged) {
            Pic.PIC_ActivateIRQ(12);
        } else {
            if (Dosbox.machine== MachineType.MCH_PCJR) Pic.PIC_ActivateIRQ(6);
            else Pic.PIC_ActivateIRQ(1);
        }
    }

    private static void KEYBOARD_AddBuffer(/*Bit8u*/int data) {
        if (keyb.used>=KEYBUFSIZE) {
            Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"Buffer full, dropping code");
            return;
        }
        /*Bitu*/int start=keyb.pos+keyb.used;
        if (start>=KEYBUFSIZE) start-=KEYBUFSIZE;
        keyb.buffer[start]=data;
        keyb.used++;
        /* Start up an event to start the first IRQ */
        if (!keyb.scheduled && !keyb.p60changed) {
            keyb.scheduled=true;
            Pic.PIC_AddEvent(KEYBOARD_TransferBuffer,KEYDELAY);
        }
    }

    static void KEYBOARD_SetLEDs(int bits) {
        /* TODO: Maybe someday you could have DOSBox show the LEDs */
        //LOG(LOG_KEYBOARD,LOG_NORMAL)("Keyboard LEDs: SCR=%u NUM=%u CAPS=%u",bits&1,(bits>>1)&1,(bits>>2)&1);
    }

    private static IoHandler.IO_ReadHandler read_p60 = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            if (keyb.buf8042_len != 0) {
                int result = keyb.buf8042[keyb.buf8042_pos];
                if (++keyb.buf8042_pos >= keyb.buf8042_len) {
                    keyb.buf8042_len = keyb.buf8042_pos = 0;
                    keyb.p60changed = false;
                    keyb.auxchanged = false;
                }
                return result & 0xFF;
            }
            keyb.p60changed=false;
            keyb.auxchanged = false;
            if (!keyb.scheduled && keyb.used!=0) {
                keyb.scheduled=true;
                Pic.PIC_AddEvent(KEYBOARD_TransferBuffer,KEYDELAY);
            }
            return keyb.p60data;
        }
    };

    static void KEYBOARD_AUX_Write(int val) {
        if (keyb.ps2mouse.type == MOUSE_NONE)
            return;

        if (keyb.ps2mouse.mode == MM_WRAP) {
            if (val != 0xFF && val != 0xEC) {
                KEYBOARD_Add8042Response(AUX|val);
                return;
            }
        }

        switch (keyb.aux_command) {
            case ACMD_NONE:
                switch (val) {
                    case 0xff:	/* reset */
                        Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"AUX reset");
                        KEYBOARD_AddBuffer(AUX|0xfa);	/* ack */
                        KEYBOARD_AddBuffer(AUX|0xaa);
                        KEYBOARD_AddBuffer(AUX|0x0);	/* mouse type */
                        Main.Mouse_AutoLock(false);
                        AUX_Reset();
                        break;
                    case 0xf6:	/* set defaults */
                        KEYBOARD_AddBuffer(AUX|0xfa);	/* ack */
                        AUX_Reset();
                        break;
                    case 0xf5:	/* disable data reporting */
                        KEYBOARD_AddBuffer(AUX|0xfa);	/* ack */
                        keyb.ps2mouse.reporting = false;
                        break;
                    case 0xf4:	/* enable data reporting */
                        KEYBOARD_AddBuffer(AUX|0xfa);	/* ack */
                        keyb.ps2mouse.reporting = true;
                        Main.Mouse_AutoLock(true);
                        break;
                    case 0xf3:	/* set sample rate */
                        KEYBOARD_AddBuffer(AUX|0xfa);	/* ack */
                        keyb.aux_command = ACMD_SET_RATE;
                        break;
                    case 0xf2:	/* get device ID */
                        KEYBOARD_AddBuffer(AUX|0xfa);	/* ack */

                        /* and then the ID */
                        if (keyb.ps2mouse.intellimouse_btn45)
                            KEYBOARD_AddBuffer(AUX|0x04);
                        else if (keyb.ps2mouse.intellimouse_mode)
                            KEYBOARD_AddBuffer(AUX|0x03);
                        else
                            KEYBOARD_AddBuffer(AUX|0x00);
                        break;
                    case 0xf0:	/* set remote mode */
                        KEYBOARD_AddBuffer(AUX|0xfa);	/* ack */
                        keyb.ps2mouse.mode = MM_REMOTE;
                        break;
                    case 0xee:	/* set wrap mode */
                        KEYBOARD_AddBuffer(AUX|0xfa);	/* ack */
                        keyb.ps2mouse.mode = MM_WRAP;
                        break;
                    case 0xec:	/* reset wrap mode */
                        KEYBOARD_AddBuffer(AUX|0xfa);	/* ack */
                        keyb.ps2mouse.mode = MM_REMOTE;
                        break;
                    case 0xeb:	/* read data */
                        KEYBOARD_AddBuffer(AUX|0xfa);	/* ack */
                        KEYBOARD_AUX_Event(0,0,
                            ((keyb.ps2mouse.m?1:0) << 2)|
                            ((keyb.ps2mouse.r?1:0) << 1)|
                            ((keyb.ps2mouse.l?1:0) << 0));
                        break;
                    case 0xea:	/* set stream mode */
                        KEYBOARD_AddBuffer(AUX|0xfa);	/* ack */
                        keyb.ps2mouse.mode = MM_STREAM;
                        break;
                    case 0xe9:	/* status request */
                        KEYBOARD_AddBuffer(AUX|0xfa);	/* ack */
                        KEYBOARD_AddBuffer(AUX|
                            (keyb.ps2mouse.mode == MM_REMOTE ? 0x40 : 0x00)|
                            ((keyb.ps2mouse.reporting?1:0) << 5)|
                            ((keyb.ps2mouse.scale21?1:0) << 4)|
                            ((keyb.ps2mouse.m?1:0) << 2)|
                            ((keyb.ps2mouse.r?1:0) << 1)|
                            ((keyb.ps2mouse.l?1:0) << 0));
                        KEYBOARD_AddBuffer(AUX|keyb.ps2mouse.resolution);
                        KEYBOARD_AddBuffer(AUX|keyb.ps2mouse.samplerate);
                        break;
                    case 0xe8:	/* set resolution */
                        KEYBOARD_AddBuffer(AUX|0xfa);	/* ack */
                        keyb.aux_command = ACMD_SET_RESOLUTION;
                        break;
                    case 0xe7:	/* set scaling 2:1 */
                        KEYBOARD_AddBuffer(AUX|0xfa);	/* ack */
                        keyb.ps2mouse.scale21 = true;
                        Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"PS/2 mouse scaling 2:1");
                        break;
                    case 0xe6:	/* set scaling 1:1 */
                        KEYBOARD_AddBuffer(AUX|0xfa);	/* ack */
                        keyb.ps2mouse.scale21 = false;
                        Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"PS/2 mouse scaling 1:1");
                        break;
                }
                break;
            case ACMD_SET_RATE:
                KEYBOARD_AddBuffer(AUX|0xfa);	/* ack */
                keyb.ps2mouse.last_srate[0] = keyb.ps2mouse.last_srate[1];
                keyb.ps2mouse.last_srate[1] = keyb.ps2mouse.last_srate[2];
                keyb.ps2mouse.last_srate[2] = val;
                keyb.ps2mouse.samplerate = val;
                keyb.aux_command = ACMD_NONE;
                Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"PS/2 mouse sample rate set to "+val);
                if (keyb.ps2mouse.type >= MOUSE_INTELLIMOUSE) {
                    if (keyb.ps2mouse.last_srate[0] == 200 && keyb.ps2mouse.last_srate[2] == 80) {
                        if (keyb.ps2mouse.last_srate[1] == 100) {
                            if (!keyb.ps2mouse.intellimouse_mode) {
                                Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"Intellimouse mode enabled");
                                keyb.ps2mouse.intellimouse_mode=true;
                            }
                        }
                        else if (keyb.ps2mouse.last_srate[1] == 200 && keyb.ps2mouse.type >= MOUSE_INTELLIMOUSE45) {
                            if (!keyb.ps2mouse.intellimouse_btn45) {
                                Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"Intellimouse 4/5-button mode enabled");
                                keyb.ps2mouse.intellimouse_btn45=true;
                            }
                        }
                    }
                }
                break;
            case ACMD_SET_RESOLUTION:
                keyb.aux_command = ACMD_NONE;
                KEYBOARD_AddBuffer(AUX|0xfa);	/* ack */
                keyb.ps2mouse.resolution = val & 3;
                Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"PS/2 mouse resolution set to "+(1 << (val&3)));
                break;
        }
    }

    private static IoHandler.IO_WriteHandler write_p60 = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            switch (keyb.command) {
            case KeyCommands.CMD_NONE:	/* None */
                if (keyb.reset)
			        return;

                /* No active command this would normally get sent to the keyboard then */
                KEYBOARD_ClrBuffer();
                switch (val) {
                case 0xed:	/* Set Leds */
                    keyb.command=KeyCommands.CMD_SETLEDS;
                    KEYBOARD_Add8042Response(0xfa);	/* Acknowledge */
                    break;
                case 0xee:	/* Echo */
                    KEYBOARD_Add8042Response(0xfa);	/* Acknowledge */
                    break;
                case 0xf0:	/* set scancode set */
                    keyb.command=KeyCommands.CMD_SETSCANSET;
                    KEYBOARD_AddBuffer(0xfa);	/* Acknowledge */
                    break;
                case 0xf2:	/* Identify keyboard */
                    /* AT's just send acknowledge */
                    KEYBOARD_AddBuffer(0xfa);	/* Acknowledge */
                    KEYBOARD_AddBuffer(0xab);	/* ID */
                    KEYBOARD_AddBuffer(0x41);
                    break;
                case 0xf3: /* Typematic rate programming */
                    keyb.command=KeyCommands.CMD_SETTYPERATE;
                    KEYBOARD_Add8042Response(0xfa);	/* Acknowledge */
                    break;
                case 0xf4:	/* Enable keyboard,clear buffer, start scanning */
                    Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"Clear buffer,enable Scaning");
                    KEYBOARD_Add8042Response(0xfa);	/* Acknowledge */
                    keyb.scanning=true;
                    break;
                case 0xf5:	 /* Reset keyboard and disable scanning */
                    Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"Reset, disable scanning");
                    keyb.scanning=false;
                    KEYBOARD_Add8042Response(0xfa);	/* Acknowledge */
                    break;
                case 0xf6:	/* Reset keyboard and enable scanning */
                    Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"Reset, enable scanning");
                    KEYBOARD_Add8042Response(0xfa);	/* Acknowledge */
                    keyb.scanning=true;
                    break;
                //case 0:
                case 0xff:		/* keyboard resets take a long time (about 250ms), most keyboards flash the LEDs during reset */
                    KEYBOARD_Reset();
                    KEYBOARD_Add8042Response(0xFA);	/* ACK */
                    KEYBOARD_Add8042Response(0xAA);
                    //keyb.reset=true;
                    KEYBOARD_SetLEDs(7); /* most keyboard I test with tend to flash the LEDs during reset */
                    //Pic.PIC_AddEvent(KEYBOARD_ResetDelay,RESETDELAY);
                    break;
                case 0x05:
                    KEYBOARD_Add8042Response(0xFE); // Resend
                    break;
                default:
                    /* Just always acknowledge strange commands */
                    if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_ERROR,"60:Unhandled command "+Integer.toString(val,16));
                    KEYBOARD_AddBuffer(0xfa);	/* Acknowledge */
                }
                return;
            case KeyCommands.CMD_SETSCANSET:
                keyb.command=KeyCommands.CMD_NONE;
                if (val == 0) { /* just asking */
                    if (keyb.cb_xlat) {
                        switch (keyb.scanset) {
                            case 1:	KEYBOARD_AddBuffer(0x43); break;
                            case 2:	KEYBOARD_AddBuffer(0x41); break;
                            case 3:	KEYBOARD_AddBuffer(0x3F); break;
                        }
                    }
                    else {
                        KEYBOARD_AddBuffer(keyb.scanset);
                    }
                    KEYBOARD_AddBuffer(0xfa);	/* Acknowledge */
                }
                else {
                    KEYBOARD_AddBuffer(0xfa);	/* Acknowledge */
                    KEYBOARD_AddBuffer(0xfa);	/* Acknowledge again */
                    if (val > 3) val = 3;
                    keyb.scanset = val;
                }
                break;
            case KeyCommands.CMD_WRITEAUX:
                keyb.command=KeyCommands.CMD_NONE;
                KEYBOARD_AUX_Write(val);
                break;
            case KeyCommands.CMD_WRITEOUTPUT:
                keyb.command=KeyCommands.CMD_NONE;
                KEYBOARD_ClrBuffer();
                KEYBOARD_AddBuffer(val);	/* and now you return the byte as if it were typed in */
                break;
            case KeyCommands.CMD_WRITEAUXOUT:
                KEYBOARD_AddBuffer(AUX|val); /* stuff into AUX output */
                break;
            case KeyCommands.CMD_SETOUTPORT:
                Memory.MEM_A20_Enable((val & 2)>0);
                keyb.command = KeyCommands.CMD_NONE;
                break;
            case KeyCommands.CMD_SETTYPERATE:
                {
                    final int delay[] = { 250, 500, 750, 1000 };
                    final int repeat[] =
                        { 33,37,42,46,50,54,58,63,67,75,83,92,100,
                          109,118,125,133,149,167,182,200,217,233,
                          250,270,303,333,370,400,435,476,500 };
                    keyb.repeat.pause = delay[(val>>5)&3];
                    keyb.repeat.rate = repeat[val&0x1f];
                    keyb.command=KeyCommands.CMD_NONE;
                }
                /* Fallthrough! as setleds does what we want */
            case KeyCommands.CMD_SETLEDS:
                if (keyb.reset)
			        return;
                keyb.command=KeyCommands.CMD_NONE;
                KEYBOARD_ClrBuffer();
                KEYBOARD_AddBuffer(0xfa);	/* Acknowledge */
                KEYBOARD_SetLEDs(val&7);
                break;
            case KeyCommands.CMD_SETCOMMAND: /* 8042 command, not keyboard */
                keyb.command=KeyCommands.CMD_NONE;
                keyb.cb_xlat = ((val >> 6) & 1)!=0;
                keyb.auxactive = ((val >> 5) & 1)==0;
                keyb.active = ((val >> 4) & 1)==0;
                keyb.cb_sys = ((val >> 2) & 1)!=0;
                keyb.cb_irq12 = ((val >> 1) & 1)!=0;
                keyb.cb_irq1 = ((val >> 0) & 1)!=0;
                if (keyb.used!=0 && !keyb.scheduled && !keyb.p60changed && keyb.active) {
                    keyb.scheduled=true;
                    Pic.PIC_AddEvent(KEYBOARD_TransferBuffer,KEYDELAY);
                }
                break;
            case 0xF4:
                keyb.scanning = true;
                break;
            }
        }
    };

    static /*Bit8u*/short port_61_data = 0;
    private static IoHandler.IO_ReadHandler read_p61 = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            port_61_data^=0x20;
            port_61_data^=0x10;
            return port_61_data;
        }
    };

    private static IoHandler.IO_WriteHandler write_p61 = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            if (((port_61_data ^ val) & 3)!=0) {
                if(((port_61_data ^ val) & 1)!=0) Timer.TIMER_SetGate2((val&0x1)!=0);
                PCSpeaker.PCSPEAKER_SetType(val & 3);
            }
            port_61_data = (short)val;
        }
    };

    private static int aux_warning = 0;

    private static IoHandler.IO_WriteHandler write_p64 = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            switch (val) {
            case 0x20:		/* read command byte */
                KEYBOARD_Add8042Response((
                    ((keyb.cb_xlat?1:0) << 6)      | ((keyb.auxactive?0:1) << 5) |
                    ((keyb.active?0:1) << 4)    | ((keyb.cb_sys?1:0) << 2) |
                    ((keyb.cb_irq12?1:0) << 1)     |  (keyb.cb_irq1?1:0)));
                break;
            case 0x60:
                keyb.command=KeyCommands.CMD_SETCOMMAND;
                break;
            case 0x90: case 0x91: case 0x92: case 0x93: case 0x94: case 0x95: case 0x96: case 0x97:
            case 0x98: case 0x99: case 0x9a: case 0x9b: case 0x9c: case 0x9d: case 0x9e: case 0x9f:
                /* TODO: If bit 0 == 0, trigger system reset */
                break;
            case 0xa7:		/* disable aux */
                if (keyb.enable_aux) {
                    keyb.auxactive=false;
                    Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"AUX De-Activated");
                }
                break;
            case 0xa8:		/* enable aux */
                if (keyb.enable_aux) {
                    keyb.auxactive=true;
                    if (keyb.used!=0 && !keyb.scheduled && !keyb.p60changed) {
                        keyb.scheduled=true;
                        Pic.PIC_AddEvent(KEYBOARD_TransferBuffer,KEYDELAY);
                    }
                    Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"AUX Activated");
                }
                break;
            case 0xa9:		/* mouse interface test */
                KEYBOARD_Add8042Response(0x00); /* OK */
                break;
            case 0xaa:		/* Self test */
                keyb.active=false; /* on real h/w it also seems to disable the keyboard */
                keyb.status = keyb.status | KBD_STAT_SELFTEST;
                KEYBOARD_Add8042Response(0x55); /* OK */
                break;
            case 0xab:      /* Keyboard interface test */
                keyb.active=false; /* on real h/w it also seems to disable the keyboard */
                KEYBOARD_Add8042Response(0);
                break;
            case 0xae:		/* Activate keyboard */
                keyb.active=true;
                if (keyb.used!=0 && !keyb.scheduled && !keyb.p60changed) {
                    keyb.scheduled=true;
                    Pic.PIC_AddEvent(KEYBOARD_TransferBuffer,KEYDELAY);
                }
                Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"Activated");
                break;
            case 0xad:		/* Deactivate keyboard */
                keyb.active=false;
                Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"De-Activated");
                break;
            case 0xc0:		/* read input buffer */
                KEYBOARD_Add8042Response(0x40);
                break;
            case 0xd0:		/* Outport on buffer */
                KEYBOARD_SetPort60((short)(Memory.MEM_A20_Enabled() ? 0x02 : 0));
                break;
            case 0xd1:		/* Write to outport */
                keyb.command=KeyCommands.CMD_SETOUTPORT;
                break;
            case 0xd2:		/* write output register */
                keyb.command=KeyCommands.CMD_WRITEOUTPUT;
                break;
            case 0xd3:		/* write AUX output */
                if (keyb.enable_aux)
                    keyb.command=KeyCommands.CMD_WRITEAUXOUT;
                else if (aux_warning++ == 0)
                    Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL, "Program is writing 8042 AUX. If you intend to use PS/2 mouse emulation you may consider adding aux=1 to your dosbox.conf");
                break;
            case 0xd4:		/* send byte to AUX */
                if (keyb.enable_aux)
                    keyb.command=KeyCommands.CMD_WRITEAUX;
                break;
            case 0xe0:		/* read test port */
                KEYBOARD_Add8042Response(0x00);
                break;
            case 0xf0: case 0xf1: case 0xf2: case 0xf3: case 0xf4: case 0xf5: case 0xf6: case 0xf7:
            case 0xf8: case 0xf9: case 0xfa: case 0xfb: case 0xfc: case 0xfd: case 0xfe: case 0xff:
                /* pulse output register */
                /* TODO: If bit 0 == 0, trigger system reset */
                if ((val & 1) == 0) {
                    System.out.println("RESET");
                    throw new Dos_programs.RebootException();
                }
                break;
            default:
                if (Log.level<=LogSeverities.LOG_ERROR) Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_ERROR,"Port 64 write with val "+val);
                break;
            }
        }
    };

    private static IoHandler.IO_ReadHandler read_p64 = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            return keyb.status | (keyb.p60changed? 0x1 : 0x0) | (keyb.auxchanged?0x20:0x00);
        }
    };

    static public void KEYBOARD_AddKey(int keytype,boolean pressed) {
        /*Bit8u*/short ret=0;boolean extend=false;
        switch (keytype) {
        case KBD_KEYS.KBD_esc:ret=1;break;
        case KBD_KEYS.KBD_1:ret=2;break;
        case KBD_KEYS.KBD_2:ret=3;break;
        case KBD_KEYS.KBD_3:ret=4;break;
        case KBD_KEYS.KBD_4:ret=5;break;
        case KBD_KEYS.KBD_5:ret=6;break;
        case KBD_KEYS.KBD_6:ret=7;break;
        case KBD_KEYS.KBD_7:ret=8;break;
        case KBD_KEYS.KBD_8:ret=9;break;
        case KBD_KEYS.KBD_9:ret=10;break;
        case KBD_KEYS.KBD_0:ret=11;break;

        case KBD_KEYS.KBD_minus:ret=12;break;
        case KBD_KEYS.KBD_equals:ret=13;break;
        case KBD_KEYS.KBD_backspace:ret=14;break;
        case KBD_KEYS.KBD_tab:ret=15;break;

        case KBD_KEYS.KBD_q:ret=16;break;
        case KBD_KEYS.KBD_w:ret=17;break;
        case KBD_KEYS.KBD_e:ret=18;break;
        case KBD_KEYS.KBD_r:ret=19;break;
        case KBD_KEYS.KBD_t:ret=20;break;
        case KBD_KEYS.KBD_y:ret=21;break;
        case KBD_KEYS.KBD_u:ret=22;break;
        case KBD_KEYS.KBD_i:ret=23;break;
        case KBD_KEYS.KBD_o:ret=24;break;
        case KBD_KEYS.KBD_p:ret=25;break;

        case KBD_KEYS.KBD_leftbracket:ret=26;break;
        case KBD_KEYS.KBD_rightbracket:ret=27;break;
        case KBD_KEYS.KBD_enter:ret=28;break;
        case KBD_KEYS.KBD_leftctrl:ret=29;break;

        case KBD_KEYS.KBD_a:ret=30;break;
        case KBD_KEYS.KBD_s:ret=31;break;
        case KBD_KEYS.KBD_d:ret=32;break;
        case KBD_KEYS.KBD_f:ret=33;break;
        case KBD_KEYS.KBD_g:ret=34;break;
        case KBD_KEYS.KBD_h:ret=35;break;
        case KBD_KEYS.KBD_j:ret=36;break;
        case KBD_KEYS.KBD_k:ret=37;break;
        case KBD_KEYS.KBD_l:ret=38;break;

        case KBD_KEYS.KBD_semicolon:ret=39;break;
        case KBD_KEYS.KBD_quote:ret=40;break;
        case KBD_KEYS.KBD_grave:ret=41;break;
        case KBD_KEYS.KBD_leftshift:ret=42;break;
        case KBD_KEYS.KBD_backslash:ret=43;break;
        case KBD_KEYS.KBD_z:ret=44;break;
        case KBD_KEYS.KBD_x:ret=45;break;
        case KBD_KEYS.KBD_c:ret=46;break;
        case KBD_KEYS.KBD_v:ret=47;break;
        case KBD_KEYS.KBD_b:ret=48;break;
        case KBD_KEYS.KBD_n:ret=49;break;
        case KBD_KEYS.KBD_m:ret=50;break;

        case KBD_KEYS.KBD_comma:ret=51;break;
        case KBD_KEYS.KBD_period:ret=52;break;
        case KBD_KEYS.KBD_slash:ret=53;break;
        case KBD_KEYS.KBD_rightshift:ret=54;break;
        case KBD_KEYS.KBD_kpmultiply:ret=55;break;
        case KBD_KEYS.KBD_leftalt:ret=56;break;
        case KBD_KEYS.KBD_space:ret=57;break;
        case KBD_KEYS.KBD_capslock:ret=58;break;

        case KBD_KEYS.KBD_f1:ret=59;break;
        case KBD_KEYS.KBD_f2:ret=60;break;
        case KBD_KEYS.KBD_f3:ret=61;break;
        case KBD_KEYS.KBD_f4:ret=62;break;
        case KBD_KEYS.KBD_f5:ret=63;break;
        case KBD_KEYS.KBD_f6:ret=64;break;
        case KBD_KEYS.KBD_f7:ret=65;break;
        case KBD_KEYS.KBD_f8:ret=66;break;
        case KBD_KEYS.KBD_f9:ret=67;break;
        case KBD_KEYS.KBD_f10:ret=68;break;

        case KBD_KEYS.KBD_numlock:ret=69;break;
        case KBD_KEYS.KBD_scrolllock:ret=70;break;

        case KBD_KEYS.KBD_kp7:ret=71;break;
        case KBD_KEYS.KBD_kp8:ret=72;break;
        case KBD_KEYS.KBD_kp9:ret=73;break;
        case KBD_KEYS.KBD_kpminus:ret=74;break;
        case KBD_KEYS.KBD_kp4:ret=75;break;
        case KBD_KEYS.KBD_kp5:ret=76;break;
        case KBD_KEYS.KBD_kp6:ret=77;break;
        case KBD_KEYS.KBD_kpplus:ret=78;break;
        case KBD_KEYS.KBD_kp1:ret=79;break;
        case KBD_KEYS.KBD_kp2:ret=80;break;
        case KBD_KEYS.KBD_kp3:ret=81;break;
        case KBD_KEYS.KBD_kp0:ret=82;break;
        case KBD_KEYS.KBD_kpperiod:ret=83;break;

        case KBD_KEYS.KBD_extra_lt_gt:ret=86;break;
        case KBD_KEYS.KBD_f11:ret=87;break;
        case KBD_KEYS.KBD_f12:ret=88;break;

        //The Extended keys

        case KBD_KEYS.KBD_kpenter:extend=true;ret=28;break;
        case KBD_KEYS.KBD_rightctrl:extend=true;ret=29;break;
        case KBD_KEYS.KBD_kpdivide:extend=true;ret=53;break;
        case KBD_KEYS.KBD_rightalt:extend=true;ret=56;break;
        case KBD_KEYS.KBD_home:extend=true;ret=71;break;
        case KBD_KEYS.KBD_up:extend=true;ret=72;break;
        case KBD_KEYS.KBD_pageup:extend=true;ret=73;break;
        case KBD_KEYS.KBD_left:extend=true;ret=75;break;
        case KBD_KEYS.KBD_right:extend=true;ret=77;break;
        case KBD_KEYS.KBD_end:extend=true;ret=79;break;
        case KBD_KEYS.KBD_down:extend=true;ret=80;break;
        case KBD_KEYS.KBD_pagedown:extend=true;ret=81;break;
        case KBD_KEYS.KBD_insert:extend=true;ret=82;break;
        case KBD_KEYS.KBD_delete:extend=true;ret=83;break;
        case KBD_KEYS.KBD_pause:
            KEYBOARD_AddBuffer(0xe1);
            KEYBOARD_AddBuffer(29|(pressed?0:0x80));
            KEYBOARD_AddBuffer(69|(pressed?0:0x80));
            return;
        case KBD_KEYS.KBD_printscreen:
            /* Not handled yet. But usuable in mapper for special events */
            return;
        default:
            Log.exit("Unsupported key press");
            break;
        }
        /* Add the actual key in the keyboard queue */
        if (keyb != null) { // keyb is  null when WINE is running
            if (pressed) {
                if (keyb.repeat.key==keytype) keyb.repeat.wait=keyb.repeat.rate;
                else keyb.repeat.wait=keyb.repeat.pause;
                keyb.repeat.key=keytype;
            } else {
                if (keyb.repeat.key == keytype) {
                    /* repeated key being released */
                    keyb.repeat.key = KBD_KEYS.KBD_NONE;
                    keyb.repeat.wait = 0;
                }
                ret+=128;
            }
            if (extend) KEYBOARD_AddBuffer(0xe0);
            KEYBOARD_AddBuffer(ret);
        }
    }

    static private Timer.TIMER_TickHandler KEYBOARD_TickHandler = new Timer.TIMER_TickHandler() {
        public void call() {
            if (keyb.repeat.wait!=0) {
                keyb.repeat.wait--;
                if (keyb.repeat.wait==0) KEYBOARD_AddKey(keyb.repeat.key,true);
            }
        }
    };

    public static Section.SectionFunction KEYBOARD_ShutDown = new Section.SectionFunction() {
        public void call(Section section) {
            IoHandler.IO_FreeWriteHandler(0x60,IoHandler.IO_MB);
            IoHandler.IO_FreeReadHandler(0x60,IoHandler.IO_MB);
            IoHandler.IO_FreeWriteHandler(0x61,IoHandler.IO_MB);
            IoHandler.IO_FreeReadHandler(0x61,IoHandler.IO_MB);
            IoHandler.IO_FreeWriteHandler(0x64,IoHandler.IO_MB);
            IoHandler.IO_FreeReadHandler(0x64,IoHandler.IO_MB);
            Timer.TIMER_DelTickHandler(KEYBOARD_TickHandler);
            KEYBOARD_ClrBuffer();
            keyb = null;
        }
    };

    public static Section.SectionFunction KEYBOARD_Init = new Section.SectionFunction() {
        public void call(Section sec) {
            keyb = new Keyb();
            Section_prop section=(Section_prop)(sec);
            if (keyb.enable_aux=section.Get_bool("aux")) {
                Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"Keyboard AUX emulation enabled");
            }
            keyb.ps2mouse.int33_taken = false;

            String sbtype=section.Get_string("auxdevice");
            keyb.ps2mouse.type = MOUSE_NONE;
            if (sbtype != null) {
                if (sbtype.equals("2button"))
                    keyb.ps2mouse.type=MOUSE_2BUTTON;
                else if (sbtype.equals("3button"))
                    keyb.ps2mouse.type=MOUSE_3BUTTON;
                else if (sbtype.equals("intellimouse"))
                    keyb.ps2mouse.type=MOUSE_INTELLIMOUSE;
                else if (sbtype.equals("intellimouse45"))
                    keyb.ps2mouse.type=MOUSE_INTELLIMOUSE45;
                else if (sbtype.equals("none"))
                    keyb.ps2mouse.type=MOUSE_NONE;
                else {
                    keyb.ps2mouse.type=MOUSE_INTELLIMOUSE;
                    Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_ERROR, "Assuming PS/2 intellimouse, I don't know what '"+sbtype+"' is");
                }
            }
            IoHandler.IO_RegisterWriteHandler(0x60,write_p60,IoHandler.IO_MB);
            IoHandler.IO_RegisterReadHandler(0x60,read_p60,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(0x61,write_p61,IoHandler.IO_MB);
            IoHandler.IO_RegisterReadHandler(0x61,read_p61,IoHandler.IO_MB);
            IoHandler.IO_RegisterWriteHandler(0x64,write_p64,IoHandler.IO_MB);
            IoHandler.IO_RegisterReadHandler(0x64,read_p64,IoHandler.IO_MB);
            Timer.TIMER_AddTickHandler(KEYBOARD_TickHandler);
            write_p61.call(0,0,0);
            KEYBOARD_Reset();
	        AUX_Reset();
            if (section != null)
                section.AddDestroyFunction(KEYBOARD_ShutDown,false);
        }
    };

    static private void AUX_Reset() {
        keyb.ps2mouse.mode = MM_STREAM;
        keyb.ps2mouse.acx = 0;
        keyb.ps2mouse.acy = 0;
        keyb.ps2mouse.samplerate = 80;
        keyb.ps2mouse.last_srate[0] = keyb.ps2mouse.last_srate[1] = keyb.ps2mouse.last_srate[2] = 0;
        keyb.ps2mouse.intellimouse_btn45 = false;
        keyb.ps2mouse.intellimouse_mode = false;
        keyb.ps2mouse.reporting = false;
        keyb.ps2mouse.scale21 = false;
        keyb.ps2mouse.resolution = 0;
        if (keyb.ps2mouse.type != MOUSE_NONE && keyb.ps2mouse.int33_taken)
            Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"PS/2 mouse emulation: taking over from INT 33h");
        keyb.ps2mouse.int33_taken = false;
        keyb.ps2mouse.l = keyb.ps2mouse.m = keyb.ps2mouse.r = false;
    }

    static public void AUX_INT33_Takeover() {
        if (keyb.ps2mouse.type != MOUSE_NONE && keyb.ps2mouse.int33_taken)
            Log.log(LogTypes.LOG_KEYBOARD, LogSeverities.LOG_NORMAL,"PS/2 mouse emulation: Program is using INT 33h, disabling direct AUX emulation");
        keyb.ps2mouse.int33_taken = true;
    }

    static private void KEYBOARD_Reset() {
        /* Init the keyb struct */
        keyb.active=true;
        keyb.scanning=true;
        //keyb.pending_key=-1;
        keyb.auxactive=false;
        //keyb.pending_key_state=false;
        keyb.command=KeyCommands.CMD_NONE;
        keyb.aux_command=ACMD_NONE;
        keyb.p60changed=false;
        keyb.auxchanged=false;
        //keyb.repeat.key=KBD_NONE;
        keyb.repeat.pause=500;
        keyb.repeat.rate=33;
        keyb.repeat.wait=0;
        keyb.scanset=1;
        /* command byte */
        keyb.cb_irq12=false;
        keyb.cb_irq1=true;
        keyb.cb_xlat=true;
        keyb.cb_sys=true;
        keyb.reset=false;
        /* OK */
        KEYBOARD_ClrBuffer();
        KEYBOARD_SetLEDs(0);
    }

}
