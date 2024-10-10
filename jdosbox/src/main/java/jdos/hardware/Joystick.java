package jdos.hardware;

import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.sdl.JavaMapper;

public class Joystick extends Module_base {
    static private final class JoystickType {
        static public final int JOY_NONE=0;
        static public final int JOY_AUTO=1;
        static public final int JOY_2AXIS=2;
        static public final int JOY_4AXIS=3;
        static public final int JOY_4AXIS_2=4;
        static public final int JOY_FCS=5;
        static public final int JOY_CH=6;
    }

    static final private int RANGE = 64;
    static final private int TIMEOUT = 10;

    static final private int OHMS = 120000/2;
    static final private float JOY_S_CONSTANT = 0.0000242f;
    static final private float S_PER_OHM = 0.000000011f;

    private static class Stick {
        boolean enabled;
        float xpos,ypos;
        double xtick,ytick;
        /*Bitu*/int xcount,ycount;
        boolean[] button=new boolean[2];
    }

    int joytype;
    static Stick[] stick=new Stick[2];

    static private /*Bit32u*/long last_write = 0;
    static private  boolean write_active = false;
    static private  boolean swap34 = false;
    static private  boolean button_wrapping_enabled = true;


    static private IoHandler.IO_ReadHandler read_p201 = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            /* Reset Joystick to 0 after TIMEOUT ms */
            if(write_active && ((Pic.PIC_Ticks - last_write) > TIMEOUT)) {
                write_active = false;
                stick[0].xcount = 0;
                stick[1].xcount = 0;
                stick[0].ycount = 0;
                stick[1].ycount = 0;
        //		LOG_MSG("reset by time %d %d",PIC_Ticks,last_write);
            }

            /**  Format of the byte to be returned:
            **                        | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
            **                        +-------------------------------+
            **                          |   |   |   |   |   |   |   |
            **  Joystick B, Button 2 ---+   |   |   |   |   |   |   +--- Joystick A, X Axis
            **  Joystick B, Button 1 -------+   |   |   |   |   +------- Joystick A, Y Axis
            **  Joystick A, Button 2 -----------+   |   |   +----------- Joystick B, X Axis
            **  Joystick A, Button 1 ---------------+   +--------------- Joystick B, Y Axis
            **/
            /*Bit8u*/short ret=0xff;
            if (stick[0].enabled) {
                if (stick[0].xcount!=0) stick[0].xcount--; else ret&=~1;
                if (stick[0].ycount!=0) stick[0].ycount--; else ret&=~2;
                if (stick[0].button[0]) ret&=~16;
                if (stick[0].button[1]) ret&=~32;
            }
            if (stick[1].enabled) {
                if (stick[1].xcount!=0) stick[1].xcount--; else ret&=~4;
                if (stick[1].ycount!=0) stick[1].ycount--; else ret&=~8;
                if (stick[1].button[0]) ret&=~64;
                if (stick[1].button[1]) ret&=~128;
            }
            return ret;
        }
    };

    static private IoHandler.IO_ReadHandler read_p201_timed = new IoHandler.IO_ReadHandler() {
        public /*Bitu*/int call(/*Bitu*/int port, /*Bitu*/int iolen) {
            /*Bit8u*/short ret=0xff;
            double currentTick = Pic.PIC_FullIndex();
            if( stick[0].enabled ){
                if( stick[0].xtick < currentTick ) ret &=~1;
                if( stick[0].ytick < currentTick ) ret &=~2;
            }
            if( stick[1].enabled ){
                if( stick[1].xtick < currentTick ) ret &=~4;
                if( stick[1].ytick < currentTick ) ret &=~8;
            }

            if (stick[0].enabled) {
                if (stick[0].button[0]) ret&=~16;
                if (stick[0].button[1]) ret&=~32;
            }
            if (stick[1].enabled) {
                if (stick[1].button[0]) ret&=~64;
                if (stick[1].button[1]) ret&=~128;
            }
            return ret;
        }
    };

    static private IoHandler.IO_WriteHandler write_p201 = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            /* Store writetime index */
            write_active = true;
            last_write = Pic.PIC_Ticks;
            if (stick[0].enabled) {
                stick[0].xcount=(/*Bitu*/int)((stick[0].xpos*RANGE)+RANGE);
                stick[0].ycount=(/*Bitu*/int)((stick[0].ypos*RANGE)+RANGE);
            }
            if (stick[1].enabled) {
                stick[1].xcount=(/*Bitu*/int)(((swap34? stick[1].ypos : stick[1].xpos)*RANGE)+RANGE);
                stick[1].ycount=(/*Bitu*/int)(((swap34? stick[1].xpos : stick[1].ypos)*RANGE)+RANGE);
            }
        }
    };

    static private IoHandler.IO_WriteHandler write_p201_timed = new IoHandler.IO_WriteHandler() {
        public void call(/*Bitu*/int port, /*Bitu*/int val, /*Bitu*/int iolen) {
            // Store writetime index
            // Axes take time = 24.2 microseconds + ( 0.011 microsecons/ohm * resistance )
            // to reset to 0
            // Precalculate the time at which each axis hits 0 here
            double currentTick = Pic.PIC_FullIndex();
            if (stick[0].enabled) {
                stick[0].xtick = currentTick + 1000.0*( JOY_S_CONSTANT + S_PER_OHM *
                                     (double)(((stick[0].xpos+1.0)* OHMS)) );
                stick[0].ytick = currentTick + 1000.0*( JOY_S_CONSTANT + S_PER_OHM *
                                 (double)(((stick[0].ypos+1.0)* OHMS)) );
            }
            if (stick[1].enabled) {
                stick[1].xtick = currentTick + 1000.0*( JOY_S_CONSTANT + S_PER_OHM *
                                 (double)((swap34? stick[1].ypos : stick[1].xpos)+1.0) * OHMS);
                stick[1].ytick = currentTick + 1000.0*( JOY_S_CONSTANT + S_PER_OHM *
                                 (double)((swap34? stick[1].xpos : stick[1].ypos)+1.0) * OHMS);
            }
        }
    };

    public static void JOYSTICK_Enable(/*Bitu*/int which,boolean enabled) {
        if (which<2) stick[which].enabled=enabled;
    }

    public static void JOYSTICK_Button(/*Bitu*/int which,/*Bitu*/int num,boolean pressed) {
        if ((which<2) && (num<2)) stick[which].button[num]=pressed;
    }

    public static void JOYSTICK_Move_X(/*Bitu*/int which,float x) {
        if (which<2) {
            stick[which].xpos=x;
        }
    }

    public static void JOYSTICK_Move_Y(/*Bitu*/int which,float y) {
        if (which<2) {
            stick[which].ypos=y;
        }
    }

    public static boolean JOYSTICK_IsEnabled(/*Bitu*/int which) {
        if (which<2) return stick[which].enabled;
        return false;
    }

    public static boolean JOYSTICK_GetButton(/*Bitu*/int which, /*Bitu*/int num) {
        if ((which<2) && (num<2)) return stick[which].button[num];
        return false;
    }

    public static float JOYSTICK_GetMove_X(/*Bitu*/int which) {
        if (which<2) return stick[which].xpos;
        return 0.0f;
    }

    public static float JOYSTICK_GetMove_Y(/*Bitu*/int which) {
        if (which<2) return stick[which].ypos;
        return 0.0f;
    }

	private IoHandler.IO_ReadHandleObject ReadHandler = new IoHandler.IO_ReadHandleObject();
	private IoHandler.IO_WriteHandleObject WriteHandler = new IoHandler.IO_WriteHandleObject();

    static Joystick test;

    public static Section.SectionFunction JOYSTICK_Destroy = new Section.SectionFunction() {
        public void call(Section section) {
            test = null;
            for (int i=0;i<stick.length;i++)
                stick[i] = null;
        }
    };

    
    public Joystick(Section configuration) {
        super(configuration);
        Section_prop section=(Section_prop)configuration;
		String type=section.Get_string("joysticktype");
		if (type.equalsIgnoreCase("none"))       joytype = JoystickType.JOY_NONE;
		else if (type.equalsIgnoreCase("false")) joytype = JoystickType.JOY_NONE;
		else if (type.equalsIgnoreCase("auto"))  joytype = JoystickType.JOY_AUTO;
		else if (type.equalsIgnoreCase("2axis")) joytype = JoystickType.JOY_2AXIS;
		else if (type.equalsIgnoreCase("4axis")) joytype = JoystickType.JOY_4AXIS;
		else if (type.equalsIgnoreCase("4axis_2")) joytype = JoystickType.JOY_4AXIS_2;
		else if (type.equalsIgnoreCase("fcs"))   joytype = JoystickType.JOY_FCS;
		else if (type.equalsIgnoreCase("ch"))    joytype = JoystickType.JOY_CH;
		else joytype = JoystickType.JOY_AUTO;

		boolean timed = section.Get_bool("timed");
		if(timed) {
			ReadHandler.Install(0x201,read_p201_timed,IoHandler.IO_MB);
			WriteHandler.Install(0x201,write_p201_timed,IoHandler.IO_MB);
		} else {
			ReadHandler.Install(0x201,read_p201,IoHandler.IO_MB);
			WriteHandler.Install(0x201,write_p201,IoHandler.IO_MB);
		}
		JavaMapper.autofire = section.Get_bool("autofire");
		swap34 = section.Get_bool("swap34");
		button_wrapping_enabled = section.Get_bool("buttonwrap");
		stick[0].enabled = false;
		stick[1].enabled = false;
		stick[0].xtick = stick[0].ytick = stick[1].xtick =
		                 stick[1].ytick = Pic.PIC_FullIndex();
    }
    public static Section.SectionFunction JOYSTICK_Init = new Section.SectionFunction() {
        public void call(Section section) {
            for (int i=0;i<stick.length;i++)
                stick[i] = new Stick();
            test = new Joystick(section);
	        section.AddDestroyFunction(JOYSTICK_Destroy,true);
        }
    };
}
