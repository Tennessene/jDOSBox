package jdos.gui;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.core_dynamic.Compiler;
import jdos.cpu.core_dynamic.Loader;
import jdos.dos.Dos_execute;
import jdos.dos.Dos_programs;
import jdos.hardware.Keyboard;
import jdos.hardware.mame.RasterizerCompiler;
import jdos.misc.Cross;
import jdos.misc.Log;
import jdos.misc.setup.*;
import jdos.sdl.GUI;
import jdos.sdl.JavaMapper;

import java.io.File;
import java.util.Vector;

public class MainBase {
    static protected GUI gui = null;

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

    static public /*Bitu*/int GFX_GetRGB(/*Bit8u*/int red,/*Bit8u*/int green,/*Bit8u*/int blue) {
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
            title.append(", CPU speed: max ");
            title.append(internal_cycles);
            title.append("% cycles, Frameskip ");
            title.append(internal_frameskip);
            title.append(", Program: ");
            title.append(Dos_execute.RunningProgram);
        } else {
            //sprintf(title,"DOSBox %s, Cpu speed: %8d cycles, Frameskip %2d, Program: %8s",VERSION,internal_cycles,internal_frameskip,RunningProgram);
            title.append("DOSBox ");
            title.append(Config.VERSION);
            title.append(", CPU speed: ");
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
    static public class ShutdownException extends RuntimeException{}
    static public class KillException extends RuntimeException{}

    static public void addEvent(Object o) {
        if (!paused) {
            events.add(o);
        }
    }

    static public boolean mouse_locked = false;
    static public float mouse_sensitivity = 100.0f;
    static protected boolean mouse_autoenable = false;
    static protected boolean mouse_autolock = false;
    static protected boolean mouse_requestlock = false;

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
    
    static final public Object pauseMutex = new Object();
    static protected void handle(FocusChangeEvent event) {
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
            } else {
                SetPriority(priority_nofocus);
            }
        }
    }

    final static public Object paintMutex = new Object();

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
            if (pressed)
                gui.fullScreenToggle();
        }
    };

    protected static boolean paused = false;
    public static boolean keyboardPaused = false;
    private static Mapper.MAPPER_Handler PauseDOSBox = new  Mapper.MAPPER_Handler() {
        public void call(boolean pressed) {
            if (!pressed) {
                return;
            }
            GFX_SetTitle(-1,-1,true);
            synchronized (pauseMutex) {
                paused = true;
                keyboardPaused = true;
                try {pauseMutex.wait();} catch (Exception e){}
                paused = false;
                keyboardPaused = true;
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
            
            JavaMapper.MAPPER_AddHandler(KillSwitch, Mapper.MapKeys.MK_f9, Mapper.MMOD1, "shutdown", "ShutDown");
	        JavaMapper.MAPPER_AddHandler(CaptureMouse, Mapper.MapKeys.MK_f10, Mapper.MMOD1, "capmouse", "Cap Mouse");
	        JavaMapper.MAPPER_AddHandler(SwitchFullScreen, Mapper.MapKeys.MK_return, Mapper.MMOD2, "fullscr", "Fullscreen");
            if (Config.C_DEBUG) {
	            /* Pause binds with activate-debugger */
            } else {
	            JavaMapper.MAPPER_AddHandler(PauseDOSBox, Mapper.MapKeys.MK_pause, Mapper.MMOD2, "pause", "Pause");
            }
        }
    };

    static private void Config_Add_SDL() {
        Section_prop sdl_sec=Dosbox.control.AddSection_prop("sdl", GUI_StartUp);
        sdl_sec.AddInitFunction(JavaMapper.MAPPER_StartUp);
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

        Pstring = sdl_sec.Add_path("mapperfile",Property.Changeable.Always, JavaMapper.mapperfile);
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
        String path = Cross.CreatePlatformConfigDir() + JavaMapper.mapperfile;
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

    protected static Vector events = new Vector();
    protected static long startupTime;

    static void main(GUI g, String[] args) {
        gui = g;
        while (true) {
            CPU.initialize();
            MainBase.GFX_SetTitle(-1, -1, false);
            CommandLine com_line = new CommandLine(args);
            String saveName;

            if (com_line.FindExist("-applet", true)) {
                Dosbox.applet = true;
            }
            if ((saveName=com_line.FindString("-compile", true))!=null) {
                Compiler.saveClasses = true;
                RasterizerCompiler.saveClasses = true;
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
            if (Dosbox.control.cmdline.FindExist("-m")) {
                Section_prop dosbox_sec = (Section_prop) Dosbox.control.GetSection("dosbox");
                if (dosbox_sec != null) {
                    Property p = dosbox_sec.byname("memsize");
                    int m = Dosbox.control.cmdline.FindInt("-m");
                    if (p!=null) {
                        p.GetValue().set(m);
                    } else {
                        dosbox_sec.Add_int("memsize", m);
                    }
                }
            }

            Dosbox.control.ParseEnv();
            Dosbox.control.Init();
            Section_prop sdl_sec = (Section_prop)Dosbox.control.GetSection("sdl");
            if (Dosbox.control.cmdline.FindExist("-fullscreen") || sdl_sec.Get_bool("fullscreen")) {
                gui.fullScreenToggle();
            }
            JavaMapper.MAPPER_Init();
            while ((path=Dosbox.control.cmdline.FindString("-mapper", true))!=null) {
                JavaMapper.MAPPER_LoadBinds(path);
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
                if (!Dosbox.applet)
                    System.exit(1);
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
