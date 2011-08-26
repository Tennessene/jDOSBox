package jdos.gui;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.core_dynamic.Compiler;
import jdos.cpu.core_dynamic.Loader;
import jdos.dos.Dos_execute;
import jdos.dos.Dos_programs;
import jdos.hardware.Keyboard;
import jdos.ints.Mouse;
import jdos.misc.Cross;
import jdos.misc.Log;
import jdos.misc.setup.*;
import jdos.sdl.GUI;
import jdos.sdl.SDL_Mapper;
import jdos.types.LogSeverities;
import jdos.types.LogTypes;
import jdos.util.IntRef;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.*;
import java.io.File;
import java.util.Vector;

public class Main {
    static private GUI gui = null;

    static public void showProgress(String msg, int percent) {
        gui.showProgress(msg, percent);
    }
    
    public static final class GFX_CallBackFunctions_t{
        static final public int GFX_CallBackReset=0;
        static final public int GFX_CallBackStop=1;
        static final public int GFX_CallBackRedraw=2;
    }

    public static interface GFX_CallBack_t {
        public void call(int function);
    }

    static private long startTime = System.currentTimeMillis();
    // emulate SDL_GetTicks -- Gets the number of milliseconds since SDL library initialization.
    static public long GetTicks() {
        return (System.currentTimeMillis()-startTime);
    }
    // emulate SDL_GetTicks -- SDL_Delay -- Waits a specified number of milliseconds before returning.
    static public void Delay(long ms) {
        try {Thread.sleep(ms);} catch (Exception e){}
    }
    
    static private final String MAPPERFILE = "mapper-" + Config.MAJOR_VERSION + ".map";

    static public void GFX_SetPalette(/*Bitu*/int start,/*Bitu*/int count, Render.RenderPal_t.RGB[] entries, int entriesOffset) {
        for (int i=start;i<start+count;i++) {
            cmap[i] = GFX_GetRGB(entries[i].red, entries[i].green, entries[i].blue);
        }
        updateBuffers();
    }

    static public /*Bitu*/int GFX_GetRGB(/*Bit8u*/short red,/*Bit8u*/short green,/*Bit8u*/short blue) {
		return ((blue << 0) | (green << 8) | (red << 16)) | (255 << 24);	
    }

    static /*Bit32s*/int internal_cycles=0;
    static /*Bits*/int internal_frameskip=0;
    static public void GFX_SetTitle(/*Bit32s*/int cycles,/*Bits*/int frameskip,boolean paused){
        StringBuffer title = new StringBuffer();
        if(cycles != -1) internal_cycles = cycles;
        if(frameskip != -1) internal_frameskip = frameskip;
        if(CPU.CPU_CycleAutoAdjust) {
            //sprintf(title,"DOSBox %s, Cpu speed: max %3d%% cycles, Frameskip %2d, Program: %8s",VERSION,internal_cycles,internal_frameskip,RunningProgram);
            title.append("DOSBox ");
            title.append(Config.VERSION);
            title.append(", Cpu speed: max ");
            title.append(internal_cycles);
            title.append("% cycles, Frameskip ");
            title.append(internal_frameskip);
            title.append(", Program: ");
            title.append(Dos_execute.RunningProgram);
        } else {
            //sprintf(title,"DOSBox %s, Cpu speed: %8d cycles, Frameskip %2d, Program: %8s",VERSION,internal_cycles,internal_frameskip,RunningProgram);
            title.append("DOSBox ");
            title.append(Config.VERSION);
            title.append(", Cpu speed: ");
            title.append(internal_cycles);
            title.append(" cycles, Frameskip ");
            title.append(internal_frameskip);
            title.append(", Program: ");
            title.append(Dos_execute.RunningProgram);
        }

        if(paused) title.append(" PAUSED");
        if (gui != null)
            gui.setTitle(title.toString());
    }

    static public class FocusChangeEvent {
        public FocusChangeEvent(boolean hasfocus) {
            this.hasfocus = hasfocus;
        }
        public boolean hasfocus;
    }
    static private class ShutdownException extends RuntimeException{}
    static private class KillException extends RuntimeException{}

    static public void GFX_Events() {
        while (events.size()>0) {
            Object event = events.elementAt(0);
            events.removeElementAt(0);
            if (event == null)
                throw new ShutdownException();
            if (event instanceof KeyEvent)
                handle((KeyEvent)event);
            else if (event instanceof FocusChangeEvent)
                handle((FocusChangeEvent)event);
            else if (event instanceof MouseEvent)
                handle((MouseEvent)event);
        }
    }

    static public void addEvent(Object o) {
        if (paused) {
            if (o instanceof KeyEvent) {
                KeyEvent key = (KeyEvent)o;
                if (key.getKeyCode() == KeyEvent.VK_PAUSE) {
                    synchronized (pauseMutex) {
                        try {pauseMutex.notify();} catch (Exception e){}
                    }
                    return;
                }
            }
        }
        events.add(o);
    }

    static public boolean mouse_locked = false;
    static public float mouse_sensitivity = 100.0f;
    static private boolean mouse_autoenable = false;
    static private boolean mouse_autolock = false;
    static private boolean mouse_requestlock = false;

    static public void GFX_CaptureMouse() {
        mouse_locked=!mouse_locked;
        if (mouse_locked) {
            //SDL_WM_GrabInput(SDL_GRAB_ON);
            gui.showCursor(false);
            gui.captureMouse(true);
        } else {
            //SDL_WM_GrabInput(SDL_GRAB_OFF);
            gui.captureMouse(false);
            if (mouse_autoenable || !mouse_autolock) gui.showCursor(true);
        }
    }

    static public class MouseEvent2 extends MouseEvent {
        public int rel_x;
        public int rel_y;
        public float abs_x;
        public float abs_y;
        public MouseEvent2(MouseEvent event, int rel_x, int rel_y, float abs_x, float abs_y, int offX, int offY) {
            super(event.getComponent(), event.getID(), event.getWhen(), event.getModifiers(), event.getX()-offX, event.getY()-offY, event.getClickCount(), event.isPopupTrigger());
            this.rel_x = rel_x;
            this.rel_y = rel_y;
            this.abs_x = abs_x;
            this.abs_y = abs_y;
        }
    }
    static public class MouseEvent1 extends MouseEvent {
        public MouseEvent1(MouseEvent event, int offX, int offY) {
            super(event.getComponent(), event.getID(), event.getWhen(), event.getModifiers(), event.getX()-offX, event.getY()-offY, event.getClickCount(), event.isPopupTrigger());
        }
    }
    static private Point lastMouse = new Point();
    static private void handle(MouseEvent event) {
        if (event.getID() == MouseEvent.MOUSE_MOVED || event.getID() == MouseEvent.MOUSE_DRAGGED) {
            // :TODO: test relative mouse with lucas arts games (indy3, indy4)
            if (mouse_locked || !mouse_autoenable) {
                if (event instanceof MouseEvent2) {
                    MouseEvent2 event2 = (MouseEvent2)event;
                    Mouse.Mouse_CursorMoved((float)event2.rel_x*mouse_sensitivity/100.0f,
                              (float)(event2.rel_y)*mouse_sensitivity/100.0f,
                              event2.abs_x,
                              event2.abs_y,
                              mouse_locked);
                } else {
                    Mouse.Mouse_CursorMoved((float)(event.getPoint().x-lastMouse.x)*mouse_sensitivity/100.0f,
                              (float)(event.getPoint().y-lastMouse.y)*mouse_sensitivity/100.0f,
                              (float)event.getPoint().x/(screen_width-1)*mouse_sensitivity/100.0f,
                              (float)event.getPoint().y/(screen_height-1)*mouse_sensitivity/100.0f,
                              mouse_locked);
                    lastMouse = event.getPoint();
                }
            }
        } else if (event.getID() == MouseEvent.MOUSE_PRESSED) {
            if (mouse_requestlock && !mouse_locked) {
                GFX_CaptureMouse();
                // Dont pass klick to mouse handler
                return;
            }
            if (!mouse_autoenable && mouse_autolock && event.getButton() == MouseEvent.BUTTON3) {
                GFX_CaptureMouse();
                return;
            }
            if (event.getButton() == MouseEvent.BUTTON1) {
                Mouse.Mouse_ButtonPressed(0);
            } else if (event.getButton() == MouseEvent.BUTTON2) {
                Mouse.Mouse_ButtonPressed(2);
            } else if (event.getButton() == MouseEvent.BUTTON3) {
                Mouse.Mouse_ButtonPressed(1);
            }
        } else if (event.getID() == MouseEvent.MOUSE_RELEASED) {
            if (event.getButton() == MouseEvent.BUTTON1) {
                Mouse.Mouse_ButtonReleased(0);
            } else if (event.getButton() == MouseEvent.BUTTON2) {
                Mouse.Mouse_ButtonReleased(2);
            } else if (event.getButton() == MouseEvent.BUTTON3) {
                Mouse.Mouse_ButtonReleased(1);
            }
        }
    }
    
    static public Object pauseMutex = new Object();
    static private void handle(FocusChangeEvent event) {
        if (event.hasfocus) {
            SetPriority(priority_focus);
        } else {
            Keyboard.KEYBOARD_AddKey(Keyboard.KBD_KEYS.KBD_leftalt, false);
            Keyboard.KEYBOARD_AddKey(Keyboard.KBD_KEYS.KBD_rightalt, false);
            if (mouse_locked) {
                GFX_CaptureMouse();
            }
            if (priority_nofocus == PRIORITY_LEVELS.PRIORITY_LEVEL_PAUSE) {
                GFX_SetTitle(-1,-1,true);
			    Keyboard.KEYBOARD_ClrBuffer();
                synchronized (pauseMutex) {
                    try {pauseMutex.wait();} catch (Exception e){}
                }
                GFX_SetTitle(-1,-1,false);
                /* Now poke a "release ALT" command into the keyboard buffer
                 * we have to do this, otherwise ALT will 'stick' and cause
                 * problems with the app running in the DOSBox.
                 */
            } else {
                SetPriority(priority_nofocus);
            }
        }
    }
    static private void handle(KeyEvent key) {
        int result;

        switch (key.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:result=Keyboard.KBD_KEYS.KBD_esc;break;
            case KeyEvent.VK_NUMPAD1:result=Keyboard.KBD_KEYS.KBD_kp1;break;
            case KeyEvent.VK_1:result=Keyboard.KBD_KEYS.KBD_1;break;
            case KeyEvent.VK_NUMPAD2:result=Keyboard.KBD_KEYS.KBD_kp2;break;
            case KeyEvent.VK_2:result=Keyboard.KBD_KEYS.KBD_2;break;
            case KeyEvent.VK_NUMPAD3:result=Keyboard.KBD_KEYS.KBD_kp3;break;
            case KeyEvent.VK_3:result=Keyboard.KBD_KEYS.KBD_3;break;
            case KeyEvent.VK_NUMPAD4:result=Keyboard.KBD_KEYS.KBD_kp4;break;
            case KeyEvent.VK_4:result=Keyboard.KBD_KEYS.KBD_4;break;
            case KeyEvent.VK_NUMPAD5:result=Keyboard.KBD_KEYS.KBD_kp5;break;
            case KeyEvent.VK_5:result=Keyboard.KBD_KEYS.KBD_5;break;
            case KeyEvent.VK_NUMPAD6:result=Keyboard.KBD_KEYS.KBD_kp6;break;
            case KeyEvent.VK_6:result=Keyboard.KBD_KEYS.KBD_6;break;
            case KeyEvent.VK_NUMPAD7:result=Keyboard.KBD_KEYS.KBD_kp7;break;
            case KeyEvent.VK_7:result=Keyboard.KBD_KEYS.KBD_7;break;
            case KeyEvent.VK_NUMPAD8:result=Keyboard.KBD_KEYS.KBD_kp8;break;
            case KeyEvent.VK_8:result=Keyboard.KBD_KEYS.KBD_8;break;
            case KeyEvent.VK_NUMPAD9:result=Keyboard.KBD_KEYS.KBD_kp9;break;
            case KeyEvent.VK_9:result=Keyboard.KBD_KEYS.KBD_9;break;
            case KeyEvent.VK_NUMPAD0:result=Keyboard.KBD_KEYS.KBD_kp0;break;
            case KeyEvent.VK_0:result=Keyboard.KBD_KEYS.KBD_0;break;
            case KeyEvent.VK_SUBTRACT:result=Keyboard.KBD_KEYS.KBD_kpminus;break;
            case KeyEvent.VK_MINUS:result=Keyboard.KBD_KEYS.KBD_minus;break;
            case KeyEvent.VK_EQUALS:result=Keyboard.KBD_KEYS.KBD_equals;break;
            case KeyEvent.VK_BACK_SPACE:result=Keyboard.KBD_KEYS.KBD_backspace;break;
            case KeyEvent.VK_TAB:result=Keyboard.KBD_KEYS.KBD_tab;break;

            case KeyEvent.VK_Q:result=Keyboard.KBD_KEYS.KBD_q;break;
            case KeyEvent.VK_W:result=Keyboard.KBD_KEYS.KBD_w;break;
            case KeyEvent.VK_E:result=Keyboard.KBD_KEYS.KBD_e;break;
            case KeyEvent.VK_R:result=Keyboard.KBD_KEYS.KBD_r;break;
            case KeyEvent.VK_T:result=Keyboard.KBD_KEYS.KBD_t;break;
            case KeyEvent.VK_Y:result=Keyboard.KBD_KEYS.KBD_y;break;
            case KeyEvent.VK_U:result=Keyboard.KBD_KEYS.KBD_u;break;
            case KeyEvent.VK_I:result=Keyboard.KBD_KEYS.KBD_i;break;
            case KeyEvent.VK_O:result=Keyboard.KBD_KEYS.KBD_o;break;
            case KeyEvent.VK_P:result=Keyboard.KBD_KEYS.KBD_p;break;

            case KeyEvent.VK_OPEN_BRACKET:result=Keyboard.KBD_KEYS.KBD_leftbracket;break;
            case KeyEvent.VK_CLOSE_BRACKET:result=Keyboard.KBD_KEYS.KBD_rightbracket;break;
            case KeyEvent.VK_ENTER:result=Keyboard.KBD_KEYS.KBD_enter;break;
            case KeyEvent.VK_CONTROL:
                if (key.getKeyLocation()==KeyEvent.KEY_LOCATION_LEFT)
                    result=Keyboard.KBD_KEYS.KBD_leftctrl;
                else
                    result=Keyboard.KBD_KEYS.KBD_rightctrl;
                break;

            case KeyEvent.VK_A:result=Keyboard.KBD_KEYS.KBD_a;break;
            case KeyEvent.VK_S:result=Keyboard.KBD_KEYS.KBD_s;break;
            case KeyEvent.VK_D:result=Keyboard.KBD_KEYS.KBD_d;break;
            case KeyEvent.VK_F:result=Keyboard.KBD_KEYS.KBD_f;break;
            case KeyEvent.VK_G:result=Keyboard.KBD_KEYS.KBD_g;break;
            case KeyEvent.VK_H:result=Keyboard.KBD_KEYS.KBD_h;break;
            case KeyEvent.VK_J:result=Keyboard.KBD_KEYS.KBD_j;break;
            case KeyEvent.VK_K:result=Keyboard.KBD_KEYS.KBD_k;break;
            case KeyEvent.VK_L:result=Keyboard.KBD_KEYS.KBD_l;break;

            case KeyEvent.VK_SEMICOLON:result=Keyboard.KBD_KEYS.KBD_semicolon;break;
            case KeyEvent.VK_QUOTE:result=Keyboard.KBD_KEYS.KBD_quote;break;
            case KeyEvent.VK_BACK_QUOTE:result=Keyboard.KBD_KEYS.KBD_grave;break;
            case KeyEvent.VK_SHIFT:
                if (key.getKeyLocation()==KeyEvent.KEY_LOCATION_LEFT)
                    result=Keyboard.KBD_KEYS.KBD_leftshift;
                else
                    result=Keyboard.KBD_KEYS.KBD_rightshift;
                break;
            case KeyEvent.VK_BACK_SLASH:result=Keyboard.KBD_KEYS.KBD_backslash;break;
            case KeyEvent.VK_Z:result=Keyboard.KBD_KEYS.KBD_z;break;
            case KeyEvent.VK_X:result=Keyboard.KBD_KEYS.KBD_x;break;
            case KeyEvent.VK_C:result=Keyboard.KBD_KEYS.KBD_c;break;
            case KeyEvent.VK_V:result=Keyboard.KBD_KEYS.KBD_v;break;
            case KeyEvent.VK_B:result=Keyboard.KBD_KEYS.KBD_b;break;
            case KeyEvent.VK_N:result=Keyboard.KBD_KEYS.KBD_n;break;
            case KeyEvent.VK_M:result=Keyboard.KBD_KEYS.KBD_m;break;

            case KeyEvent.VK_COMMA:result=Keyboard.KBD_KEYS.KBD_comma;break;
            case KeyEvent.VK_PERIOD:result=Keyboard.KBD_KEYS.KBD_period;break;
            case KeyEvent.VK_DECIMAL:result=Keyboard.KBD_KEYS.KBD_kpperiod;break;
            case KeyEvent.VK_SLASH:result=Keyboard.KBD_KEYS.KBD_slash;break;
            //case KeyEvent.VK_SHIFT:result=Keyboard.KBD_KEYS.KBD_rightshift;break;
            case KeyEvent.VK_MULTIPLY:result=Keyboard.KBD_KEYS.KBD_kpmultiply;break;
            case KeyEvent.VK_ALT:
                if (key.getKeyLocation()==KeyEvent.KEY_LOCATION_LEFT)
                    result=Keyboard.KBD_KEYS.KBD_leftalt;
                else
                    result=Keyboard.KBD_KEYS.KBD_rightalt;
                break;
            case KeyEvent.VK_SPACE:result=Keyboard.KBD_KEYS.KBD_space;break;
            case KeyEvent.VK_CAPS_LOCK:result=Keyboard.KBD_KEYS.KBD_capslock;break;

            case KeyEvent.VK_F1:result=Keyboard.KBD_KEYS.KBD_f1;break;
            case KeyEvent.VK_F2:result=Keyboard.KBD_KEYS.KBD_f2;break;
            case KeyEvent.VK_F3:result=Keyboard.KBD_KEYS.KBD_f3;break;
            case KeyEvent.VK_F4:result=Keyboard.KBD_KEYS.KBD_f4;break;
            case KeyEvent.VK_F5:result=Keyboard.KBD_KEYS.KBD_f5;break;
            case KeyEvent.VK_F6:result=Keyboard.KBD_KEYS.KBD_f6;break;
            case KeyEvent.VK_F7:result=Keyboard.KBD_KEYS.KBD_f7;break;
            case KeyEvent.VK_F8:result=Keyboard.KBD_KEYS.KBD_f8;break;
            case KeyEvent.VK_F9:result=Keyboard.KBD_KEYS.KBD_f9;break;
            case KeyEvent.VK_F10:result=Keyboard.KBD_KEYS.KBD_f10;break;

            case KeyEvent.VK_NUM_LOCK:result=Keyboard.KBD_KEYS.KBD_numlock;break;
            case KeyEvent.VK_SCROLL_LOCK:result=Keyboard.KBD_KEYS.KBD_scrolllock;break;

            case KeyEvent.VK_PLUS:result=Keyboard.KBD_KEYS.KBD_kpplus;break;

            case KeyEvent.VK_LESS:result=Keyboard.KBD_KEYS.KBD_extra_lt_gt;break;
            case KeyEvent.VK_F11:result=Keyboard.KBD_KEYS.KBD_f11;break;
            case KeyEvent.VK_F12:result=Keyboard.KBD_KEYS.KBD_f12;break;

            //The Extended keys

            case KeyEvent.VK_DIVIDE:result=Keyboard.KBD_KEYS.KBD_kpdivide;break;
            case KeyEvent.VK_ADD:result=Keyboard.KBD_KEYS.KBD_kpplus;break;
            case KeyEvent.VK_HOME:result=Keyboard.KBD_KEYS.KBD_home;break;
            case KeyEvent.VK_UP:result=Keyboard.KBD_KEYS.KBD_up;break;
            case KeyEvent.VK_PAGE_UP:result=Keyboard.KBD_KEYS.KBD_pageup;break;
            case KeyEvent.VK_LEFT:result=Keyboard.KBD_KEYS.KBD_left;break;
            case KeyEvent.VK_RIGHT:result=Keyboard.KBD_KEYS.KBD_right;break;
            case KeyEvent.VK_END:result=Keyboard.KBD_KEYS.KBD_end;break;
            case KeyEvent.VK_DOWN:result=Keyboard.KBD_KEYS.KBD_down;break;
            case KeyEvent.VK_PAGE_DOWN:result=Keyboard.KBD_KEYS.KBD_pagedown;break;
            case KeyEvent.VK_INSERT:result=Keyboard.KBD_KEYS.KBD_insert;break;
            case KeyEvent.VK_DELETE:result=Keyboard.KBD_KEYS.KBD_delete;break;
            case KeyEvent.VK_PAUSE:result=Keyboard.KBD_KEYS.KBD_pause;break;
            case KeyEvent.VK_PRINTSCREEN:result=Keyboard.KBD_KEYS.KBD_printscreen;break;
            default:
                if (Log.level<=LogSeverities.LOG_WARN) Log.log(LogTypes.LOG_GUI, LogSeverities.LOG_WARN, "Unknown key code: "+key.getKeyCode());
                return;
        }
        Keyboard.KEYBOARD_AddKey(result, key.getID()==KeyEvent.KEY_PRESSED);
    }

    final static public Object paintMutex = new Object();
    static private int skipCount = 0;

    static public void GFX_EndUpdate() {
        if (pixelBuffer != null) {
            if (startupTime != 0) {
                System.out.println("Startup time: "+String.valueOf(System.currentTimeMillis()-startupTime)+"ms");
                startupTime = 0;
            }
            long startTime=0;
            if (Render.render.frameskip.auto)
                startTime = System.currentTimeMillis();
            synchronized (paintMutex) {
                if (Render.render.frameskip.auto) {
                    long diff = System.currentTimeMillis()-startTime;
                    if (diff>5) {
                        skipCount=0;
                        Render.render.frameskip.max++;
                        Main.GFX_SetTitle(-1, Render.render.frameskip.max, false);
                    } else if (diff<2 && Render.render.frameskip.max>0) {
                        skipCount++;
                        if (skipCount>100) {
                            skipCount=0;
                            Render.render.frameskip.max--;
                            Main.GFX_SetTitle(-1, Render.render.frameskip.max, false);
                        }
                    }
                }
                int type = buffer.getRaster().getTransferType();
                switch (type) {
                    case DataBuffer.TYPE_BYTE:
                    {
                        int pos = 0;
                        for (int i=0;i<pixelBuffer.length;i++) {
                            int p = pixelBuffer[i];
                            byte_rawImageData[pos++] = (byte)p;
                            byte_rawImageData[pos++] = (byte)(p >> 8);
                            byte_rawImageData[pos++] = (byte)(p >> 16);
                            byte_rawImageData[pos++] = (byte)(p >> 24);
                        }
                        break;
                    }
                    case DataBuffer.TYPE_SHORT:
                    case DataBuffer.TYPE_USHORT:
                    {
                        int pos = 0;
                        for (int i=0;i<pixelBuffer.length;i++) {
                            int p = pixelBuffer[i];
                            short_rawImageData[pos++] = (short)(p);
                            short_rawImageData[pos++] = (short)(p >> 16);
                        }
                        break;
                    }
                    case DataBuffer.TYPE_INT:
                        System.arraycopy(pixelBuffer, 0, int_rawImageData, 0, pixelBuffer.length);
                        break;
                }
            }
            gui.dopaint();
        }
    }
    static byte[] byte_rawImageData;
    static short[] short_rawImageData;
    static int[] int_rawImageData;
    static int[] pixelBuffer;
    static int pitch;

    static public boolean GFX_StartUpdate(int[][] pixels,/*Bitu*/IntRef pitch) {
        pixels[0] = pixelBuffer;
        pitch.value = Main.pitch;
        return true;
    }

    static int screen_width;
    static int screen_height;
    static int buffer_width;
    static int buffer_height;
    static IndexColorModel colorModel;
    static int[] cmap = new int[256];

    static {
        int i=0;
        for (int r=0; r < 256; r += 51) {
            for (int g=0; g < 256; g += 51) {
                for (int b=0; b < 256; b += 51) {
                    cmap[i++] = (r<<16)|(g<<8)|b;
                }
            }
        }
        // And populate the rest of the cmap with gray values
        int grayIncr = 256/(256-i);

        // The gray ramp will be between 18 and 252
        int gray = grayIncr*3;
        for (; i < 256; i++) {
            cmap[i] = (gray<<16)|(gray<<8)|gray;
            gray += grayIncr;
        }
    }
    static BufferedImage buffer;
    static public /*Bitu*/void GFX_SetSize(/*Bitu*/int width,/*Bitu*/int height,GFX_CallBack_t callback) {
        buffer_width = screen_width = width;
        buffer_height = screen_height = height;
        if (Render.render.aspect) {
            screen_height = screen_width*3/4;
        }
        gui.setSize(screen_width, screen_height);
        if (Render.render.src.dblh)
            buffer_height /= 2;
        if (Render.render.src.dblw)
            buffer_width /= 2;
        updateBuffers();
    }

    static private void updateBuffers() {
        switch (Render.render.src.bpp) {
            case 8:
            {
                colorModel = new IndexColorModel(8, 256, cmap, 0, false, -1, DataBuffer.TYPE_BYTE);
                buffer = new BufferedImage(buffer_width, buffer_height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
                DataBufferByte buf = (DataBufferByte) buffer.getRaster().getDataBuffer();
                byte_rawImageData = buf.getData();
                pixelBuffer = new int[byte_rawImageData.length>>2];
                pitch = buffer_width;
            }
                break;
            case 15:
            {
                buffer = new BufferedImage(buffer_width, buffer_height, BufferedImage.TYPE_USHORT_555_RGB);
                DataBufferUShort buf = (DataBufferUShort) buffer.getRaster().getDataBuffer();
                short_rawImageData = buf.getData();
                pixelBuffer = new int[short_rawImageData.length>>1];
                pitch = buffer_width*2;
            }
                break;
            case 16:
            {
                buffer = new BufferedImage(buffer_width, buffer_height, BufferedImage.TYPE_USHORT_565_RGB);
                DataBufferUShort buf = (DataBufferUShort) buffer.getRaster().getDataBuffer();
                short_rawImageData = buf.getData();
                pixelBuffer = new int[short_rawImageData.length>>1];
                pitch = buffer_width*2;
            }
                break;
            case 32:
            {
                buffer = new BufferedImage(buffer_width, buffer_height, BufferedImage.TYPE_INT_RGB);
                DataBufferInt buf = (DataBufferInt) buffer.getRaster().getDataBuffer();
                int_rawImageData = buf.getData();
                pixelBuffer = new int[int_rawImageData.length];
                pitch = buffer_width*4;
            }
                break;
        }
    }

    static public void Mouse_AutoLock(boolean enable) {
        mouse_autolock=enable;
        if (mouse_autoenable) mouse_requestlock=enable;
        else {
            gui.showCursor(!enable);
            mouse_requestlock=false;
        }
    }

    static void SetPriority(int level) {
        if (true) return;
        switch (level) {
        case PRIORITY_LEVELS.PRIORITY_LEVEL_PAUSE:	// if DOSBox is paused, assume idle priority
        case PRIORITY_LEVELS.PRIORITY_LEVEL_LOWEST:
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            break;
        case PRIORITY_LEVELS.PRIORITY_LEVEL_LOWER:
            Thread.currentThread().setPriority((Thread.NORM_PRIORITY+Thread.MIN_PRIORITY)/2);
            break;
        case PRIORITY_LEVELS.PRIORITY_LEVEL_NORMAL:
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
            break;
        case PRIORITY_LEVELS.PRIORITY_LEVEL_HIGHER:
            Thread.currentThread().setPriority((Thread.NORM_PRIORITY+Thread.MAX_PRIORITY)/2);
            break;
        case PRIORITY_LEVELS.PRIORITY_LEVEL_HIGHEST:
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            break;
        }
    }
    private static Section.SectionFunction GUI_ShutDown = new Section.SectionFunction() {
        public void call(Section section) {
            //GFX_Stop();
	        //if (sdl.draw.callback) (sdl.draw.callback)( GFX_CallBackStop );
	        if (mouse_locked) GFX_CaptureMouse();
	        //if (sdl.desktop.fullscreen) GFX_SwitchFullScreen();
        }
    };

    private static Mapper.MAPPER_Handler KillSwitch = new  Mapper.MAPPER_Handler() {
        public void call(boolean pressed) {
            if (pressed) {
                throw new KillException();
            }
        }
    };

    private static Mapper.MAPPER_Handler CaptureMouse = new  Mapper.MAPPER_Handler() {
        public void call(boolean pressed) {
            if (pressed) {
                GFX_CaptureMouse();
            }
        }
    };

    private static Mapper.MAPPER_Handler SwitchFullScreen = new  Mapper.MAPPER_Handler() {
        public void call(boolean pressed) {
            gui.fullScreenToggle();
        }
    };

    private static boolean paused = false;
    private static Mapper.MAPPER_Handler PauseDOSBox = new  Mapper.MAPPER_Handler() {
        public void call(boolean pressed) {
            if (!pressed) {
                return;
            }
            GFX_SetTitle(-1,-1,true);

            synchronized (pauseMutex) {
                paused = true;
                try {pauseMutex.wait();} catch (Exception e){}
                paused = false;
            }
            GFX_SetTitle(-1,-1,false);
        }
    };

    static final class PRIORITY_LEVELS {
        static public final int PRIORITY_LEVEL_PAUSE=0;
        static public final int PRIORITY_LEVEL_LOWEST=1;
        static public final int PRIORITY_LEVEL_LOWER=2;
        static public final int PRIORITY_LEVEL_NORMAL=3;
        static public final int PRIORITY_LEVEL_HIGHER=4;
        static public final int PRIORITY_LEVEL_HIGHEST=5;
    }

    private static int priority_focus;
    private static int priority_nofocus;
    
    private static Section.SectionFunction GUI_StartUp = new Section.SectionFunction() {
        public void call(Section sec) {
            sec.AddDestroyFunction(GUI_ShutDown);
            Section_prop section = (Section_prop)sec;

            Prop_multival p=section.Get_multival("priority");
            String focus = p.GetSection().Get_string("active");
            String notfocus = p.GetSection().Get_string("inactive");

            if      (focus.equals("lowest"))  { priority_focus = PRIORITY_LEVELS.PRIORITY_LEVEL_LOWEST;  }
            else if (focus.equals("lower"))   { priority_focus = PRIORITY_LEVELS.PRIORITY_LEVEL_LOWER;   }
            else if (focus.equals("normal"))  { priority_focus = PRIORITY_LEVELS.PRIORITY_LEVEL_NORMAL;  }
            else if (focus.equals("higher"))  { priority_focus = PRIORITY_LEVELS.PRIORITY_LEVEL_HIGHER;  }
            else if (focus.equals("highest")) { priority_focus = PRIORITY_LEVELS.PRIORITY_LEVEL_HIGHEST; }

            if      (notfocus.equals("lowest"))  { priority_nofocus=PRIORITY_LEVELS.PRIORITY_LEVEL_LOWEST;  }
            else if (notfocus.equals("lower"))   { priority_nofocus=PRIORITY_LEVELS.PRIORITY_LEVEL_LOWER;   }
            else if (notfocus.equals("normal"))  { priority_nofocus=PRIORITY_LEVELS.PRIORITY_LEVEL_NORMAL;  }
            else if (notfocus.equals("higher"))  { priority_nofocus=PRIORITY_LEVELS.PRIORITY_LEVEL_HIGHER;  }
            else if (notfocus.equals("highest")) { priority_nofocus=PRIORITY_LEVELS.PRIORITY_LEVEL_HIGHEST; }
            else if (notfocus.equals("pause"))   {
                /* we only check for pause here, because it makes no sense
                 * for DOSBox to be paused while it has focus
                 */
                priority_nofocus=PRIORITY_LEVELS.PRIORITY_LEVEL_PAUSE;
                // :TODO: test this, it will probably crash
            }
            SetPriority(priority_focus); //Assume focus on startup

            Integer autolock = Dosbox.control.cmdline.FindInt("-autolock", true);
            if (autolock != null) {
                mouse_autoenable = autolock.intValue()==1;
            } else {
                mouse_autoenable = section.Get_bool("autolock");
            }
            if (!mouse_autoenable) gui.showCursor(false);
            mouse_autolock = false;
            mouse_sensitivity = section.Get_int("sensitivity");
            
            Mapper.MAPPER_AddHandler(KillSwitch, Mapper.MapKeys.MK_f9,Mapper.MMOD1,"shutdown","ShutDown");
	        Mapper.MAPPER_AddHandler(CaptureMouse,Mapper.MapKeys.MK_f10,Mapper.MMOD1,"capmouse","Cap Mouse");
	        Mapper.MAPPER_AddHandler(SwitchFullScreen,Mapper.MapKeys.MK_return,Mapper.MMOD2,"fullscr","Fullscreen");
            if (Config.C_DEBUG) {
	            /* Pause binds with activate-debugger */
            } else {
	            Mapper.MAPPER_AddHandler(PauseDOSBox, Mapper.MapKeys.MK_pause, Mapper.MMOD2, "pause", "Pause");
            }
        }
    };

    static private void Config_Add_SDL() {
        Section_prop sdl_sec=Dosbox.control.AddSection_prop("sdl", GUI_StartUp);
        sdl_sec.AddInitFunction(SDL_Mapper.MAPPER_StartUp);
        Prop_bool Pbool;
        Prop_string Pstring;
        Prop_int Pint;
        Prop_multival Pmulti;

        Pbool = sdl_sec.Add_bool("fullscreen",Property.Changeable.Always,false);
        Pbool.Set_help("Start dosbox directly in fullscreen. (Press ALT-Enter to go back)");

        Pbool = sdl_sec.Add_bool("fulldouble",Property.Changeable.Always,false);
        Pbool.Set_help("Use double buffering in fullscreen. It can reduce screen flickering, but it can also result in a slow DOSBox.");

        Pstring = sdl_sec.Add_string("fullresolution",Property.Changeable.Always,"original");
        Pstring.Set_help("What resolution to use for fullscreen: original or fixed size (e.g. 1024x768).\n" +
                          "  Using your monitor's native resolution with aspect=true might give the best results.\n" +
                  "  If you end up with small window on a large screen, try an output different from surface.");

        Pstring = sdl_sec.Add_string("windowresolution",Property.Changeable.Always,"original");
        Pstring.Set_help("Scale the window to this size IF the output device supports hardware scaling.\n" +
                          "  (output=surface does not!)");
        String[] outputs = {"surface", "overlay","opengl", "openglnb","ddraw"};
        Pstring = sdl_sec.Add_string("output",Property.Changeable.Always,"surface");
	    Pstring.Set_help("What video system to use for output.");
	    Pstring.Set_values(outputs);

        Pbool = sdl_sec.Add_bool("autolock",Property.Changeable.Always,true);
        Pbool.Set_help("Mouse will automatically lock, if you click on the screen. (Press CTRL-F10 to unlock)");

        Pint = sdl_sec.Add_int("sensitivity",Property.Changeable.Always,100);
        Pint.SetMinMax(1,1000);
        Pint.Set_help("Mouse sensitivity.");

        Pbool = sdl_sec.Add_bool("waitonerror",Property.Changeable.Always, true);
        Pbool.Set_help("Wait before closing the console if dosbox has an error.");

        Pmulti = sdl_sec.Add_multi("priority", Property.Changeable.Always, ",");
        Pmulti.SetValue("higher,normal");
        Pmulti.Set_help("Priority levels for dosbox. Second entry behind the comma is for when dosbox is not focused/minimized.\n" +
                         "  pause is only valid for the second entry.");

        String[] actt = { "lowest", "lower", "normal", "higher", "highest", "pause"};
        Pstring = Pmulti.GetSection().Add_string("active",Property.Changeable.Always,"higher");
        Pstring.Set_values(actt);

        String inactt[] = { "lowest", "lower", "normal", "higher", "highest", "pause"};
        Pstring = Pmulti.GetSection().Add_string("inactive",Property.Changeable.Always,"normal");
        Pstring.Set_values(inactt);

        Pstring = sdl_sec.Add_path("mapperfile",Property.Changeable.Always,MAPPERFILE);
        Pstring.Set_help("File used to load/save the key/event mappings from. Resetmapper only works with the defaul value.");

        Pbool = sdl_sec.Add_bool("usescancodes",Property.Changeable.Always,true);
        Pbool.Set_help("Avoid usage of symkeys, might not work on all operating systems.");
    }

    static void launcheditor() {
        String path = Cross.CreatePlatformConfigDir() + Cross.GetPlatformConfigName();
        if (!Dosbox.control.PrintConfig(path)) {
            Log.exit("tried creating "+path+". but failed.\n");
        }

        String edit;
        while((edit = Dosbox.control.cmdline.FindString("-editconf",true)) != null) { //Loop until one succeeds
            try {
                Process p = Runtime.getRuntime().exec(new String[] {edit,path});
                if (p != null)
                    System.exit(0);
            } catch (Exception e) {

            }
        }
        //if you get here the launching failed!
        Log.exit("can't find editor(s) specified at the command line.\n");
    }

    static void launchcaptures(String edit) {
        String file = null;
        Section t = Dosbox.control.GetSection("dosbox");
        if(t != null) file = t.GetPropValue("captures");
        if(t == null || file.equals(Section.NO_SUCH_PROPERTY)) {
            Log.exit("Config system messed up.\n");
        }
        String path = Cross.CreatePlatformConfigDir();
        path += file;
        Cross.CreateDir(path);
        if(new File(path).isDirectory()) {
            Log.exit(path+" doesn't exists or isn't a directory.\n");
        }
    /*	if(edit.empty()) {
            printf("no editor specified.\n");
            exit(1);
        }*/

        try {
            Process p = Runtime.getRuntime().exec(new String[] {edit,path});
            if (p != null)
                System.exit(0);
        } catch (Exception e) {

        }
        //if you get here the launching failed!
        Log.exit("can't find filemanager "+edit+"\n");
    }

    static void eraseconfigfile() {
        if(new File("dosbox.conf").exists()) {
            show_warning("Warning: dosbox.conf exists in current working directory.\nThis will override the configuration file at runtime.\n");
        }
        String path = Cross.CreatePlatformConfigDir() + Cross.GetPlatformConfigName();
        new File(path).delete();
        System.exit(0);
    }

    static void erasemapperfile() {
        if(new File("dosbox.conf").exists()) {
            show_warning("Warning: dosbox.conf exists in current working directory.\nKeymapping might not be properly reset.\n" +
                         "Please reset configuration as well and delete the dosbox.conf.\n");
        }
        String path = Cross.CreatePlatformConfigDir() + MAPPERFILE;
        new File(path).delete();
        System.exit(0);
    }

    static void show_warning(String message) {
        // :TODO:
        Log.log_msg(message);
    }

    static void printconfiglocation() {
        String path = Cross.CreatePlatformConfigDir() + Cross.GetPlatformConfigName();
        if (!Dosbox.control.PrintConfig(path)) {
            Log.exit("tried creating "+path+". but failed.\n");
        }
        Log.log_msg(path+"\n");
        System.exit(0);
    }

    private static Vector events = new Vector();

    private static long startupTime;

    static void main(GUI g, String[] args) {
        gui = g;
        while (true) {
            Main.GFX_SetTitle(-1, -1, false);
            CommandLine com_line = new CommandLine(args);
            String saveName;

            if (com_line.FindExist("-applet", true)) {
                Dosbox.applet = true;
            }
            if ((saveName=com_line.FindString("-compile", true))!=null) {
                Compiler.saveClasses = true;
            }

            Config myconf = new Config(com_line);
            Dosbox.control = myconf;
            Config_Add_SDL();
            Dosbox.Init();
            String captures;
            if (Dosbox.control.cmdline.FindString("-editconf", false) != null) launcheditor();
            if ((captures = Dosbox.control.cmdline.FindString("-opencaptures", true)) != null) launchcaptures(captures);
            if (Dosbox.control.cmdline.FindExist("-eraseconf")) eraseconfigfile();
            if (Dosbox.control.cmdline.FindExist("-resetconf")) eraseconfigfile();
            if (Dosbox.control.cmdline.FindExist("-erasemapper")) erasemapperfile();
            if (Dosbox.control.cmdline.FindExist("-resetmapper")) erasemapperfile();
            // For now just use the java console, in the future we could open a separate swing windows and redirect to there if necessary
            if (Dosbox.control.cmdline.FindExist("-version") || Dosbox.control.cmdline.FindExist("--version")) {
                Log.log_msg("\nDOSBox version "+Config.VERSION+", copyright 2002-2010 DOSBox Team.\n\n");
                Log.log_msg("DOSBox is written by the DOSBox Team (See AUTHORS file))\n");
                Log.log_msg("DOSBox comes with ABSOLUTELY NO WARRANTY.  This is free software,\n");
                Log.log_msg("and you are welcome to redistribute it under certain conditions;\n");
                Log.log_msg("please read the COPYING file thoroughly before doing so.\n\n");
                return;
            }
            if (Dosbox.control.cmdline.FindExist("-printconf")) printconfiglocation();
            Log.log_msg("DOSBox version "+Config.VERSION);
            Log.log_msg("Copyright 2002-2010 DOSBox Team, published under GNU GPL.");
            Log.log_msg("---");


            /* Parse configuration files */
            boolean parsed_anyconfigfile = false;
            //First Parse -userconf
            if (Dosbox.control.cmdline.FindExist("-userconf", true)) {
                String path = Cross.CreatePlatformConfigDir() + Cross.GetPlatformConfigName();
                if (Dosbox.control.ParseConfigFile(path)) parsed_anyconfigfile = true;
                if (!parsed_anyconfigfile) {
                    //Try to create the userlevel configfile.
                    if (Dosbox.control.PrintConfig(path)) {
                        Log.log_msg("CONFIG: Generating default configuration.\nWriting it to "+path);
                        //Load them as well. Makes relative paths much easier
                        if (Dosbox.control.ParseConfigFile(path)) parsed_anyconfigfile = true;
                    }
                }
            }
            //Second parse -conf entries
            String path;
            while ((path=Dosbox.control.cmdline.FindString("-conf", true))!=null) {
                if (Dosbox.control.ParseConfigFile(path)) parsed_anyconfigfile = true;
            }
            if (!Dosbox.applet) {
                //if none found => parse localdir conf
                if (!parsed_anyconfigfile)
                    if (Dosbox.control.ParseConfigFile("dosbox.conf")) parsed_anyconfigfile = true;
                //if none found => parse userlevel conf
                if (!parsed_anyconfigfile) {
                    path = Cross.CreatePlatformConfigDir() + Cross.GetPlatformConfigName();
                    if (Dosbox.control.ParseConfigFile(path)) parsed_anyconfigfile = true;
                }
                if (!parsed_anyconfigfile) {
                    path = Cross.CreatePlatformConfigDir() + Cross.GetPlatformConfigName();
                    if (Dosbox.control.PrintConfig(path)) {
                        Log.log_msg("CONFIG: Generating default configuration.\nWriting it to "+path);
                        //Load them as well. Makes relative paths much easier
                        Dosbox.control.ParseConfigFile(path);
                    } else {
                        Log.log_msg("CONFIG: Using default settings. Create a configfile to change them");
                    }
                }
            }
            Dosbox.control.ParseEnv();
            Dosbox.control.Init();
            Section_prop sdl_sec = (Section_prop)Dosbox.control.GetSection("sdl");
            if (Dosbox.control.cmdline.FindExist("-fullscreen") || sdl_sec.Get_bool("fullscreen")) {
                gui.fullScreenToggle();
            }

            /* Start up main machine */
            try {
                startupTime = System.currentTimeMillis();
                Dosbox.control.StartUp();
            } catch (Dos_programs.RebootException e) {
                System.out.println("Rebooting");
                try {myconf.Destroy();} catch (Exception e1){}
                continue;
            } catch (ShutdownException e) {
                if (saveName!=null) {
                    Loader.save(saveName, false);
                }
                System.out.println("Normal Shutdown");
                try {myconf.Destroy();} catch (Exception e1){}
            } catch (KillException e) {
                System.out.println("Normal Shutdown");
            } catch (Exception e) {
                e.printStackTrace();
                if (!Dosbox.applet)
                    System.exit(1);
            } finally {
                events.clear();
            }
            break;
        }
    }
}
