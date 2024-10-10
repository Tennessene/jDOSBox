package jdos;

import jdos.cpu.*;
import jdos.cpu.core_dynamic.Compiler;
import jdos.debug.Debug;
import jdos.debug.Debug_gui;
import jdos.dos.*;
import jdos.fpu.FPU;
import jdos.gui.Main;
import jdos.gui.Mapper;
import jdos.gui.Midi;
import jdos.gui.Render;
import jdos.hardware.*;
import jdos.hardware.DMA;
import jdos.hardware.VGA;
import jdos.hardware.mame.VoodooCommon;
import jdos.hardware.pci.PCI;
import jdos.hardware.qemu.*;
import jdos.hardware.serialport.Serialports;
import jdos.ints.*;
import jdos.misc.Log;
import jdos.misc.Msg;
import jdos.misc.Program;
import jdos.misc.setup.*;
import jdos.sdl.JavaMapper;
import jdos.shell.Shell;
import jdos.types.MachineType;
import jdos.types.SVGACards;

public class Dosbox {
    static public boolean allPrivileges = true;

    static private interface LoopHandler {
        public /*Bitu*/int call();
    }
    public static boolean applet = false;
    public static Config control;
    public static int machine;
    public static int svgaCard;
    private static LoopHandler loop;
    public static boolean SDLNetInited;

    private static /*Bit32u*/long ticksRemain;
    private static /*Bit32u*/long ticksLast;
    private static /*Bit32u*/long ticksAdded;
    public static /*Bit32s*/int ticksDone;
    public static /*Bit32u*/long ticksScheduled;
    public static boolean ticksLocked;

    static public boolean IS_TANDY_ARCH() {
        return ((machine==MachineType.MCH_TANDY) || (machine==MachineType.MCH_PCJR));
    }

    static public boolean IS_EGAVGA_ARCH() {
        return ((machine==MachineType.MCH_EGA) || (machine==MachineType.MCH_VGA));
    }

    static public boolean IS_VGA_ARCH() {
        return (machine==MachineType.MCH_VGA);
    }

    static private LoopHandler Normal_Loop = new LoopHandler() {
         public /*Bitu*/int call() {
            /*Bits*/int ret;
            while (true) {
                if (Pic.PIC_RunQueue()) {
                    ret=CPU.cpudecoder.call();
                    if (ret<0) return 1;
                    if (ret>0) {
                        //if (ret >= Callback.CB_MAX) return 0;
                        try {
                            Callback.inHandler++;
//                            if (ret != 15 && ret !=7 && ret != 19 && ret != 16)
//                               System.out.println(Callback.CallBack_Handlers[ret].getName());
                            /*Bitu*/int blah=Callback.CallBack_Handlers[ret].call();
                            if (blah!=0) return blah;
                        } catch(PageFaultException e) {
                            if (e.shouldRunException) {
                                Paging.pageFault = true;
                                CPU.CPU_Exception(CPU.cpu.exception.which, CPU.cpu.exception.error);
                                Paging.pageFault = false;
                            }
                            Log.exit("This should not happen");
                        } finally {
                            Callback.inHandler--;
                        }
                    }
                    if (Config.C_DEBUG)
                        if (Debug.DEBUG_ExitLoop()) return 0;
                } else {
                    Main.GFX_Events();
                    if (ticksRemain>0) {
                        Timer.TIMER_AddTick();
                        ticksRemain--;
                    } else {
                        //goto increaseticks;
                        break;
                    }
                }
            }
        //increaseticks:
            if (ticksLocked) {
                ticksRemain=5;
                /* Reset any auto cycle guessing for this frame */
                ticksLast = Main.GetTicks();
                ticksAdded = 0;
                ticksDone = 0;
                ticksScheduled = 0;
            } else {
                /*Bit32u*/long ticksNew;
                ticksNew=Main.GetTicks();
                ticksScheduled += ticksAdded;
                if (ticksNew > ticksLast) {
                    ticksRemain = ticksNew-ticksLast;
                    ticksLast = ticksNew;
                    ticksDone += ticksRemain;
                    if ( ticksRemain > 20 ) {
                        ticksRemain = 20;
                    }
                    ticksAdded = ticksRemain;
                    if (CPU.CPU_CycleAutoAdjust && !CPU.CPU_SkipCycleAutoAdjust) {
                        if (ticksScheduled >= 250 || ticksDone >= 250 || (ticksAdded > 15 && ticksScheduled >= 5) ) {
                            if(ticksDone < 1) ticksDone = 1; // Protect against div by zero
                            /* ratio we are aiming for is around 90% usage*/
                            /*Bit32s*/int ratio = (int)((ticksScheduled * (CPU.CPU_CyclePercUsed*90*1024/100/100)) / ticksDone);
                            /*Bit32s*/int new_cmax = CPU.CPU_CycleMax;
                            /*Bit64s*/long cproc = (/*Bit64s*/long)CPU.CPU_CycleMax * (/*Bit64s*/long)ticksScheduled;
                            if (cproc > 0) {
                                /* ignore the cycles added due to the io delay code in order
                                   to have smoother auto cycle adjustments */
                                double ratioremoved = (double) CPU.CPU_IODelayRemoved / (double) cproc;
                                if (ratioremoved < 1.0) {
                                    ratio = (/*Bit32s*/int)((double)ratio * (1 - ratioremoved));
                                    /* Don't allow very high ratio which can cause us to lock as we don't scale down
                                     * for very low ratios. High ratio might result because of timing resolution */
                                    if (ticksScheduled >= 250 && ticksDone < 10 && ratio > 20480)
                                        ratio = 20480;
                                    /*Bit64s*/long cmax_scaled = (/*Bit64s*/long)CPU.CPU_CycleMax * (/*Bit64s*/long)ratio;
                                    if (ratio <= 1024)
                                        new_cmax = (/*Bit32s*/int)(cmax_scaled / (/*Bit64s*/long)1024);
                                    else
                                        new_cmax = (/*Bit32s*/int)(1 + (CPU.CPU_CycleMax >> 1) + cmax_scaled / (/*Bit64s*/long)2048);
                                }
                            }

                            if (new_cmax<CPU.CPU_CYCLES_LOWER_LIMIT)
                                new_cmax=CPU.CPU_CYCLES_LOWER_LIMIT;

                            /* ratios below 1% are considered to be dropouts due to
                               temporary load imbalance, the cycles adjusting is skipped */
                            if (ratio>10) {
                                /* ratios below 12% along with a large time since the last update
                                   has taken place are most likely caused by heavy load through a
                                   different application, the cycles adjusting is skipped as well */
                                if ((ratio>120) || (ticksDone<700)) {
                                    CPU.CPU_CycleMax = new_cmax;
                                    if (CPU.CPU_CycleLimit > 0) {
                                        if (CPU.CPU_CycleMax>CPU.CPU_CycleLimit) CPU.CPU_CycleMax = CPU.CPU_CycleLimit;
                                    }
                                }
                            }
                            CPU.CPU_IODelayRemoved = 0;
                            ticksDone = 0;
                            ticksScheduled = 0;
                        } else if (ticksAdded > 15) {
                            /* ticksAdded > 15 but ticksScheduled < 5, lower the cycles
                               but do not reset the scheduled/done ticks to take them into
                               account during the next auto cycle adjustment */
                            CPU.CPU_CycleMax /= 3;
                            if (CPU.CPU_CycleMax < CPU.CPU_CYCLES_LOWER_LIMIT)
                                CPU.CPU_CycleMax = CPU.CPU_CYCLES_LOWER_LIMIT;
                        }
                    }
                } else {
                    ticksAdded = 0;
                    Main.Delay(1);
                    ticksDone -= Main.GetTicks() - ticksNew;
                    if (ticksDone < 0)
                        ticksDone = 0;
                }
            }
            return 0;
         }
    };

    static private void DOSBOX_SetLoop(LoopHandler handler) {
        loop=handler;
    }

    static public void DOSBOX_SetNormalLoop() {
        loop=Normal_Loop;
    }

    static public void DOSBOX_RunMachinePF(){
        /*Bitu*/int ret;
        do {
            ret=loop.call();
        } while (ret==0);
    }

    static public void DOSBOX_RunMachine(){
        /*Bitu*/int ret;
        do {
            try {
                ret=loop.call();
            } catch (PageFaultException e) {
                if (e.shouldRunException) {
                    Paging.pageFault = true;
                    CPU.CPU_Exception(CPU.cpu.exception.which, CPU.cpu.exception.error);
                    Paging.pageFault = false;
                }
                ret = 0;
            } catch (CPUException e) {
                ret = 0;
            }
        } while (ret==0);
    }

    static private boolean autoadjust = false;
    static private Mapper.MAPPER_Handler DOSBOX_UnlockSpeed = new Mapper.MAPPER_Handler() {
        public void call(boolean pressed) {
            if (pressed) {
                Log.log_msg("Fast Forward ON");
                ticksLocked = true;
                if (CPU.CPU_CycleAutoAdjust) {
                    autoadjust = true;
                    CPU.CPU_CycleAutoAdjust = false;
                    CPU.CPU_CycleMax /= 3;
                    if (CPU.CPU_CycleMax<1000) CPU.CPU_CycleMax=1000;
                }
            } else {
                Log.log_msg("Fast Forward OFF");
                ticksLocked = false;
                if (autoadjust) {
                    autoadjust = false;
                    CPU.CPU_CycleAutoAdjust = true;
                }
            }
        }
    };

    private static Section.SectionFunction DOSBOX_RealInit = new Section.SectionFunction() {
        public void call(Section sec) {
            System.out.println("DOSBOX_RealInit");
            Section_prop section=(Section_prop)sec;
            /* Initialize some dosbox internals */

            ticksRemain=0;
            ticksLast=Main.GetTicks();
            ticksLocked = false;
            DOSBOX_SetLoop(Normal_Loop);
            Msg.init(section);

            JavaMapper.MAPPER_AddHandler(DOSBOX_UnlockSpeed, Mapper.MapKeys.MK_f12, Mapper.MMOD2, "speedlock", "Speedlock");
            String cmd_machine;
            if ((cmd_machine=control.cmdline.FindString("-machine",true))!=null) {
                //update value in config (else no matching against suggested values
                section.HandleInputline("machine=" + cmd_machine);
            }

            String mtype = section.Get_string("machine");
            svgaCard = SVGACards.SVGA_None;
            machine = MachineType.MCH_VGA;
            Int10.int10 = new Int10.Int10Data();
            Int10.int10.vesa_nolfb = false;
            Int10.int10.vesa_oldvbe = false;
            if      (mtype.equals("cga"))      { machine = MachineType.MCH_CGA; }
            else if (mtype.equals("tandy"))    { machine = MachineType.MCH_TANDY; }
            else if (mtype.equals("pcjr"))     { machine = MachineType.MCH_PCJR; }
            else if (mtype.equals("hercules")) { machine = MachineType.MCH_HERC; }
            else if (mtype.equals("ega"))      { machine = MachineType.MCH_EGA; }
        //	else if (mtype.equals("vga")          { svgaCard = SVGA_S3Trio; }
            else if (mtype.equals("svga_s3"))       { svgaCard = SVGACards.SVGA_S3Trio; }
            else if (mtype.equals("vesa_nolfb"))   { svgaCard = SVGACards.SVGA_S3Trio; Int10.int10.vesa_nolfb = true;}
            else if (mtype.equals("vesa_oldvbe"))   { svgaCard = SVGACards.SVGA_S3Trio; Int10.int10.vesa_oldvbe = true;}
            else if (mtype.equals("svga_et4000"))   { svgaCard = SVGACards.SVGA_TsengET4K; }
            else if (mtype.equals("svga_et3000"))   { svgaCard = SVGACards.SVGA_TsengET3K; }
        //	else if (mtype.equals("vga_pvga1a")   { svgaCard = SVGA_ParadisePVGA1A; }
            else if (mtype.equals("svga_paradise")) { svgaCard = SVGACards.SVGA_ParadisePVGA1A; }
            else if (mtype.equals("vgaonly"))      { svgaCard = SVGACards.SVGA_None; }
            else if (mtype.equals("vgastd"))      { svgaCard = SVGACards.SVGA_QEMU; }
            else Log.exit("DOSBOX:Unknown machine type "+mtype);
            if (svgaCard != SVGACards.SVGA_QEMU)
                VGA.VGA_Init();
        }
    };
    
    public static void Init() {
        Section_prop secprop;
        Section_line secline;
        Prop_int Pint;
        Prop_hex Phex;
        Prop_string Pstring;
        Prop_bool Pbool;
        Prop_multival Pmulti;
        Prop_multival_remain Pmulti_remain;

        SDLNetInited = false;

        // Some frequently used option sets
        String[] rates = {  "44100", "48000", "32000","22050", "16000", "11025", "8000", "49716" };
        String[] oplrates = {   "44100", "49716", "48000", "32000","22050", "16000", "11025", "8000" };
        String[] ios = { "220", "240", "260", "280", "2a0", "2c0", "2e0", "300" };
        String[] irqssb = { "7", "5", "3", "9", "10", "11", "12" };
        String[] dmassb = { "1", "5", "0", "3", "6", "7" };
        String[] iosgus = { "240", "220", "260", "280", "2a0", "2c0", "2e0", "300" };
        String[] irqsgus = { "5", "3", "7", "9", "10", "11", "12" };
        String[] dmasgus = { "3", "0", "1", "5", "6", "7" };


        /* Setup all the different modules making up DOSBox */
        String[] machines = {
            "hercules", "cga", "tandy", "pcjr", "ega",
            "vgaonly", "svga_s3", "svga_et3000", "svga_et4000",
             "svga_paradise", "vesa_nolfb", "vesa_oldvbe", "vgastd" };
        secprop=control.AddSection_prop("dosbox",DOSBOX_RealInit);
        Pstring = secprop.Add_path("language",Property.Changeable.Always,"");
        Pstring.Set_help("Select another language file.");

        Pstring = secprop.Add_string("machine",Property.Changeable.OnlyAtStart,"svga_s3");
        Pstring.Set_values(machines);
        Pstring.Set_help("The type of machine tries to emulate.");

        Pint = secprop.Add_int("vmemsize", Property.Changeable.WhenIdle,2);
	    Pint.SetMinMax(0,8);
	    Pint.Set_help(
		"Amount of video memory in megabytes.\n" +
		"  The maximum resolution and color depth the svga_s3 will be able to display\n" +
		"  is determined by this value.\n " +
		"  0: 512k (800x600  at 256 colors)\n" +
		"  1: 1024x768  at 256 colors or 800x600  at 64k colors\n" +
		"  2: 1600x1200 at 256 colors or 1024x768 at 64k colors or 640x480 at 16M colors\n" +
		"  4: 1600x1200 at 64k colors or 1024x768 at 16M colors\n" +
		"  8: up to 1600x1200 at 16M colors\n" +
		"For build engine games, use more memory than in the list above so it can\n" +
		"use triple buffering and thus won't flicker.\n"
		);
        Pint = secprop.Add_int("vmemcachesize", 512);
        Pint.SetMinMax(0,8192);
        Pint.Set_help("The amount of video cache to use in kb.  Only certain video modes will\n"+
                      "uses this.  0 will use the Dosbox standard value 2x video memory size.");

        Pstring = secprop.Add_path("captures",Property.Changeable.Always,"capture");
        Pstring.Set_help("Directory where things like wave, midi, screenshot get captured.");

        if (Config.C_DEBUG)
            Debug_gui.LOG_StartUp();

        secprop.AddInitFunction(IO.IO_Init);//done
        secprop.AddInitFunction(Paging.PAGING_Init);//done
        secprop.AddInitFunction(Memory.MEM_Init);//done
        secprop.AddInitFunction(Hardware.HARDWARE_Init);//done
        Pint = secprop.Add_int("memsize", Property.Changeable.WhenIdle,16);
        Pint.SetMinMax(1,63);
        Pint.Set_help(
            "Amount of memory DOSBox has in megabytes.\n" +
            "  This value is best left at its default to avoid problems with some games,\n" +
            "  though few games might require a higher value.\n" +
            "  There is generally no speed advantage when raising this value.");
        secprop.AddInitFunction(Callback.CALLBACK_Init);
        secprop.AddInitFunction(Pic.PIC_Init);//done
        secprop.AddInitFunction(Program.PROGRAMS_Init);
        secprop.AddInitFunction(Timer.TIMER_Init);//done
        secprop.AddInitFunction(Cmos.CMOS_Init);//done
        secprop.AddInitFunction(VGA.VGA_Init);
        secprop.AddInitFunction(jdos.hardware.qemu.VGA.QEMU_VGA_Init);

        secprop=control.AddSection_prop("render", Render.RENDER_Init,true);
        Pint = secprop.Add_int("frameskip",Property.Changeable.Always,-1);
        Pint.SetMinMax(0,10);
        Pint.Set_help("How many frames DOSBox skips before drawing one. Use -1 for auto");

        Pbool = secprop.Add_bool("aspect",Property.Changeable.Always,false);
        Pbool.Set_help("Do aspect correction, if your output method doesn't support scaling this can slow things down!.");

        Pmulti = secprop.Add_multi("scaler",Property.Changeable.Always," ");
        Pmulti.SetValue("normal2x");
        Pmulti.Set_help("Scaler used to enlarge/enhance low resolution modes.\n" +
                         "  If 'forced' is appended, then the scaler will be used even if the result might not be desired.");
        Pstring = Pmulti.GetSection().Add_string("type",Property.Changeable.Always,"normal2x");

        String[] scalers;
        if (Render.RENDER_USE_ADVANCED_SCALERS>2)
            scalers  = new String[] {
            "none", "normal2x", "normal3x",
            "advmame2x", "advmame3x", "advinterp2x", "advinterp3x", "hq2x", "hq3x", "2xsai", "super2xsai", "supereagle",
            "tv2x", "tv3x", "rgb2x", "rgb3x", "scan2x", "scan3x"};
        else if (Render.RENDER_USE_ADVANCED_SCALERS>0)
            scalers  = new String[] {
            "none", "normal2x", "normal3x",
            "tv2x", "tv3x", "rgb2x", "rgb3x", "scan2x", "scan3x"};
        else
            scalers  = new String[] {"none", "normal2x", "normal3x"};

        Pstring.Set_values(scalers);


        String force[] = { "", "forced" };
        Pstring = Pmulti.GetSection().Add_string("force",Property.Changeable.Always,"");
        Pstring.Set_values(force);

        secprop=control.AddSection_prop("cpu", CPU.CPU_Init,true);//done
        String[] cores;
        if (Config.C_DYNAMIC || Config.C_DYNREC)
            cores = new String[] { "auto", "dynamic", "normal", "simple"};
        else
            cores = new String[] { "auto", "normal", "simple"};
        Pstring = secprop.Add_string("core",Property.Changeable.WhenIdle,"auto");
        Pstring.Set_values(cores);
        Pstring.Set_help("CPU Core used in emulation. auto will switch to dynamic if available and appropriate.");

        String[] cputype_values = { "auto", "386", "486", "pentium", "386_prefetch", "486_prefetch", "p6"};
        Pstring = secprop.Add_string("cputype",Property.Changeable.Always,"auto");
        Pstring.Set_values(cputype_values);
        Pstring.Set_help("CPU Type used in emulation. auto emulates a 486 which tolerates Pentium instructions.");


        Pmulti_remain = secprop.Add_multiremain("cycles",Property.Changeable.Always," ");
        Pmulti_remain.Set_help(
            "Amount of instructions DOSBox tries to emulate each millisecond.\n" +
            "Setting this value too high results in sound dropouts and lags.\n" +
            "Cycles can be set in 3 ways:\n" +
            "  'auto'          tries to guess what a game needs.\n" +
            "                  It usually works, but can fail for certain games.\n" +
            "  'fixed #number' will set a fixed amount of cycles. This is what you usually need if 'auto' fails.\n" +
            "                  (Example: fixed 4000).\n" +
            "  'max'           will allocate as much cycles as your computer is able to handle.\n");

        String[] cyclest = { "auto","fixed","max","%u"};
        Pstring = Pmulti_remain.GetSection().Add_string("type",Property.Changeable.Always,"auto");
        Pmulti_remain.SetValue("auto");
        Pstring.Set_values(cyclest);

        Pstring = Pmulti_remain.GetSection().Add_string("parameters",Property.Changeable.Always,"");

        Pint = secprop.Add_int("cycleup",Property.Changeable.Always,10);
        Pint.SetMinMax(1,1000000);
        Pint.Set_help("Amount of cycles to decrease/increase with keycombo.(CTRL-F11/CTRL-F12)");

        Pint = secprop.Add_int("cycledown",Property.Changeable.Always,20);
        Pint.SetMinMax(1,1000000);
        Pint.Set_help("Setting it lower than 100 will be a percentage.");

        if (Config.C_FPU) {
            secprop.AddInitFunction(FPU.FPU_Init);
            Pbool = secprop.Add_bool("softfpu",Property.Changeable.Always,false);
            Pbool.Set_help("Enable software emulation of the FPU");
        }

        if (allPrivileges) {
            secprop=control.AddSection_prop("compiler", Compiler.Compiler_Init,true);
            Pint = secprop.Add_int("threshold",Property.Changeable.Always,1000);
            Pint.Set_help("How many times a block is seen before it is compiled.  0 turns off the compiler, 1 compiles everything (And will like cause problems with self modifying code).\nOnly used when dynamic_core is active. Values between 100-1000 yield the best results.");

            Pint = secprop.Add_int("min_block_size",Property.Changeable.Always,2);
            Pint.Set_help("The minimum number of ops the block must contain in order to be compiled.  In general 2 is a good value.");
        }

        secprop.AddInitFunction(DMA.DMA_Init);//done

        if (Config.PCI_FUNCTIONALITY_ENABLED) {
            secprop=control.AddSection_prop("pci", PCI.PCI_Init,true); //PCI bus
            Pbool = secprop.Add_bool("enabled", Property.Changeable.OnlyAtStart, false);
            Pbool.Set_help("PCI needs to be enabled if you want to use a Voodoo card.\n"+
                           "Enabling PCI will most likely break Bochs Bios support");
            secprop=control.AddSection_prop("3dfx", VoodooCommon.Voodoo_Init,true); //PCI bus
            String[] types = new String[] { "none", "voodoo1", "voodoo2"};
            Pstring = secprop.Add_string("type",Property.Changeable.OnlyAtStart,"voodoo1");
            Pstring.Set_values(types);
            Pstring.Set_help(       "Which 3dfx card you would like to emulate.  Remember to enabled PCI.\n" +
                                    "  'none'\n"+
                                    "  'voodoo1'       Frame Buffer can be 2 or 4MB\n" +
                                    "                  Texture Memory can be 1, 2 or 4MB\n" +
                                    "                  Can have 1 or 2 Texture Management Units\n" +
                                    "                      singletmu is set to false by default\n" +
                                    "  'voodoo2'       Frame Buffer can be 2 or 4MB\n" +
                                    "                  Texture Memory can be 2, 4, 8 or 16MB\n" +
                                    "                  Number of Texture Managment Units (TMUs) is 2\n"+
                                    "                      singletmu is ignored\n");
            Pstring = secprop.Add_string("framebuffer", Property.Changeable.OnlyAtStart, "4");
            Pstring.Set_values(new String[]{"2", "4"});
            Pstring.Set_help("The amount of memory the framebuffer has.\n"+
                             "  2   The highest resolution is 640x480\n"+
                             "  4   The highest resolution is 800x600\n");
            Pstring = secprop.Add_string("texturememory", Property.Changeable.OnlyAtStart, "4");
            Pstring.Set_values(new String[]{"1", "2", "4", "8", "16"});
            Pstring.Set_help("The amount of memory each TMU has");
            Pbool = secprop.Add_bool("singletmu", Property.Changeable.OnlyAtStart, false);
            Pbool.Set_help("Voodoo 1 can have 1 or 2 TMUs.  2 is the default");
        }

        secprop=control.AddSection_prop("keyboard",Keyboard.KEYBOARD_Init);
        Pbool = secprop.Add_bool("aux",Property.Changeable.OnlyAtStart,true);
        Pbool.Set_help("Enable emulation of the 8042 auxiliary port. PS/2 mouse emulation requires this to be enabled");
    
        String[] auxdevices = {"none","2button","3button","intellimouse","intellimouse45"};
        Pstring = secprop.Add_string("auxdevice",Property.Changeable.OnlyAtStart,"intellimouse");
        Pstring.Set_values(auxdevices);
        Pstring.Set_help("Type of PS/2 mouse attached to the AUX port");
        
        secprop=control.AddSection_prop("mixer",Mixer.MIXER_Init);
        Pbool = secprop.Add_bool("nosound",Property.Changeable.OnlyAtStart,false);
        Pbool.Set_help("Enable silent mode, sound is still emulated though.");

        Pint = secprop.Add_int("rate",Property.Changeable.OnlyAtStart,44100);
        Pint.Set_values(rates);
        Pint.Set_help("Mixer sample rate, setting any device's rate higher than this will probably lower their sound quality.");

        String[] blocksizes = {"1024", "2048", "4096", "8192", "512", "256"};
        Pint = secprop.Add_int("blocksize",Property.Changeable.OnlyAtStart,512);
        Pint.Set_values(blocksizes);
        Pint.Set_help("Mixer block size, larger blocks might help sound stuttering but sound will also be more lagged.");

        Pint = secprop.Add_int("prebuffer",Property.Changeable.OnlyAtStart,20);
        Pint.SetMinMax(0,100);
        Pint.Set_help("How many milliseconds of data to keep on top of the blocksize.");

        Pint = secprop.Add_int("javabuffer",Property.Changeable.OnlyAtStart,8820);
        Pint.SetMinMax(0,100);
        Pint.Set_help("Buffer parameter passed to Java's SourceDataLine.open call. At 44100Hz, 16-bit stereo a value of 8820 represents 50ms");

        secprop=control.AddSection_prop("midi", Midi.MIDI_Init,true);//done
        secprop.AddInitFunction(MPU401.MPU401_Init,true);//done

        String[] mputypes = { "intelligent", "uart", "none"};
        // FIXME: add some way to offer the actually available choices.
        String[] devices = { "default", "win32", "alsa", "oss", "coreaudio", "coremidi","none"};
        Pstring = secprop.Add_string("mpu401",Property.Changeable.WhenIdle,"intelligent");
        Pstring.Set_values(mputypes);
        Pstring.Set_help("Type of MPU-401 to emulate.");

        Pstring = secprop.Add_string("mididevice",Property.Changeable.WhenIdle,"default");
        Pstring.Set_values(devices);
        Pstring.Set_help("Device that will receive the MIDI data from MPU-401.");

        Pstring = secprop.Add_string("midiconfig",Property.Changeable.WhenIdle,"");
        Pstring.Set_help("Special configuration options for the device driver. This is usually the id of the device you want to use.\n" +
                          "  When using a Roland MT-32 rev. 0 as midi output device, some games may require a delay in order to prevent 'buffer overflow' issues.\n" +
	                      "  In that case, add 'delaysysex', for example: midiconfig=2 delaysysex\n" +
                          "  See the README/Manual for more details.");

        if (Config.C_DEBUG)
            secprop=control.AddSection_prop("debug", Debug.DEBUG_Init);

        secprop=control.AddSection_prop("sblaster",SBlaster.SBLASTER_Init,true);//done

        String[] sbtypes = { "sb1", "sb2", "sbpro1", "sbpro2", "sb16", "gb", "none"};
        Pstring = secprop.Add_string("sbtype",Property.Changeable.WhenIdle,"sb16");
        Pstring.Set_values(sbtypes);
        Pstring.Set_help("Type of Soundblaster to emulate. gb is Gameblaster.");

        Phex = secprop.Add_hex("sbbase",Property.Changeable.WhenIdle,new Hex(0x220));
        Phex.Set_values(ios);
        Phex.Set_help("The IO address of the soundblaster.");

        Pint = secprop.Add_int("irq",Property.Changeable.WhenIdle,7);
        Pint.Set_values(irqssb);
        Pint.Set_help("The IRQ number of the soundblaster.");

        Pint = secprop.Add_int("dma",Property.Changeable.WhenIdle,1);
        Pint.Set_values(dmassb);
        Pint.Set_help("The DMA number of the soundblaster.");

        Pint = secprop.Add_int("hdma",Property.Changeable.WhenIdle,5);
        Pint.Set_values(dmassb);
        Pint.Set_help("The High DMA number of the soundblaster.");

        Pbool = secprop.Add_bool("sbmixer",Property.Changeable.WhenIdle,true);
        Pbool.Set_help("Allow the soundblaster mixer to modify the DOSBox mixer.");

        String[] oplmodes={ "auto", "cms", "opl2", "dualopl2", "opl3", "none"};
        Pstring = secprop.Add_string("oplmode",Property.Changeable.WhenIdle,"auto");
        Pstring.Set_values(oplmodes);
        Pstring.Set_help("Type of OPL emulation. On 'auto' the mode is determined by sblaster type. All OPL modes are Adlib-compatible, except for 'cms'.");

        String[] oplemus={ "default", "compat", "fast"};
        Pstring = secprop.Add_string("oplemu",Property.Changeable.WhenIdle,"default");
        Pstring.Set_values(oplemus);
        Pstring.Set_help("Provider for the OPL emulation. compat might provide better quality (see oplrate as well).");

        Pint = secprop.Add_int("oplrate",Property.Changeable.WhenIdle,44100);
        Pint.Set_values(oplrates);
        Pint.Set_help("Sample rate of OPL music emulation. Use 49716 for highest quality (set the mixer rate accordingly).");


        secprop=control.AddSection_prop("gus",Gus.GUS_Init,true); //done
        Pbool = secprop.Add_bool("gus",Property.Changeable.WhenIdle,false);
        Pbool.Set_help("Enable the Gravis Ultrasound emulation.");

        Pint = secprop.Add_int("gusrate",Property.Changeable.WhenIdle,44100);
        Pint.Set_values(rates);
        Pint.Set_help("Sample rate of Ultrasound emulation.");

        Phex = secprop.Add_hex("gusbase",Property.Changeable.WhenIdle,new Hex(0x240));
        Phex.Set_values(iosgus);
        Phex.Set_help("The IO base address of the Gravis Ultrasound.");

        Pint = secprop.Add_int("gusirq",Property.Changeable.WhenIdle,5);
        Pint.Set_values(irqsgus);
        Pint.Set_help("The IRQ number of the Gravis Ultrasound.");

        Pint = secprop.Add_int("gusdma",Property.Changeable.WhenIdle,3);
        Pint.Set_values(dmasgus);
        Pint.Set_help("The DMA channel of the Gravis Ultrasound.");

        Pstring = secprop.Add_string("ultradir",Property.Changeable.WhenIdle,"C:\\ULTRASND");
        Pstring.Set_help(
            "Path to Ultrasound directory. In this directory\n" +
            "there should be a MIDI directory that contains\n" +
            "the patch files for GUS playback. Patch sets used\n" +
            "with Timidity should work fine.");

        secprop = control.AddSection_prop("speaker",PCSpeaker.PCSPEAKER_Init,true);//done
        Pbool = secprop.Add_bool("pcspeaker",Property.Changeable.WhenIdle,true);
        Pbool.Set_help("Enable PC-Speaker emulation.");

        Pint = secprop.Add_int("pcrate",Property.Changeable.WhenIdle,44100);
        Pint.Set_values(rates);
        Pint.Set_help("Sample rate of the PC-Speaker sound generation.");

        secprop.AddInitFunction(TandySound.TANDYSOUND_Init,true);
        String[] tandys = { "auto", "on", "off"};
        Pstring = secprop.Add_string("tandy",Property.Changeable.WhenIdle,"auto");
        Pstring.Set_values(tandys);
        Pstring.Set_help("Enable Tandy Sound System emulation. For 'auto', emulation is present only if machine is set to 'tandy'.");

        Pint = secprop.Add_int("tandyrate",Property.Changeable.WhenIdle,44100);
        Pint.Set_values(rates);
        Pint.Set_help("Sample rate of the Tandy 3-Voice generation.");

        secprop.AddInitFunction(Disney.DISNEY_Init,true);//done

        Pbool = secprop.Add_bool("disney",Property.Changeable.WhenIdle,true);
        Pbool.Set_help("Enable Disney Sound Source emulation. (Covox Voice Master and Speech Thing compatible).");

        secprop=control.AddSection_prop("joystick", Bios.BIOS_Init,false);//done
        secprop.AddInitFunction(Int10.INT10_Init);
        secprop.AddInitFunction(Mouse.MOUSE_Init); //Must be after int10 as it uses CurMode
        secprop.AddInitFunction(Joystick.JOYSTICK_Init);
        String[] joytypes = { "auto", "2axis", "4axis", "4axis_2", "fcs", "ch", "none"};
        Pstring = secprop.Add_string("joysticktype",Property.Changeable.WhenIdle,"auto");
        Pstring.Set_values(joytypes);
        Pstring.Set_help(
            "Type of joystick to emulate: auto (default), none,\n" +
            "2axis (supports two joysticks),\n" +
            "4axis (supports one joystick, first joystick used),\n" +
            "4axis_2 (supports one joystick, second joystick used),\n" +
            "fcs (Thrustmaster), ch (CH Flightstick).\n" +
            "none disables joystick emulation.\n" +
            "auto chooses emulation depending on real joystick(s).\n" +
            "(Remember to reset dosbox's mapperfile if you saved it earlier)");

        Pbool = secprop.Add_bool("timed",Property.Changeable.WhenIdle,true);
        Pbool.Set_help("enable timed intervals for axis. Experiment with this option, if your joystick drifts (away).");

        Pbool = secprop.Add_bool("autofire",Property.Changeable.WhenIdle,false);
        Pbool.Set_help("continuously fires as long as you keep the button pressed.");

        Pbool = secprop.Add_bool("swap34",Property.Changeable.WhenIdle,false);
        Pbool.Set_help("swap the 3rd and the 4th axis. can be useful for certain joysticks.");

        Pbool = secprop.Add_bool("buttonwrap",Property.Changeable.WhenIdle,false);
        Pbool.Set_help("enable button wrapping at the number of emulated buttons.");

        secprop=control.AddSection_prop("serial", Serialports.SERIAL_Init,true);
        String[] serials = { "dummy", "disabled", "modem", "nullmodem",
                                  "directserial"};

        Pmulti_remain = secprop.Add_multiremain("serial1",Property.Changeable.WhenIdle," ");
        Pstring = Pmulti_remain.GetSection().Add_string("type",Property.Changeable.WhenIdle,"dummy");
        Pmulti_remain.SetValue("dummy");
        Pstring.Set_values(serials);
        Pstring = Pmulti_remain.GetSection().Add_string("parameters",Property.Changeable.WhenIdle,"");
        Pmulti_remain.Set_help(
            "set type of device connected to com port.\n" +
            "Can be disabled, dummy, modem, nullmodem, directserial.\n" +
            "Additional parameters must be in the same line in the form of\n" +
            "parameter:value. Parameter for all types is irq (optional).\n" +
            "for directserial: realport (required), rxdelay (optional).\n" +
            "                 (realport:COM1 realport:ttyS0).\n" +
            "for modem: listenport (optional).\n" +
            "for nullmodem: server, rxdelay, txdelay, telnet, usedtr,\n" +
            "               transparent, port, inhsocket (all optional).\n" +
            "Example: serial1=modem listenport:5000");

        Pmulti_remain = secprop.Add_multiremain("serial2",Property.Changeable.WhenIdle," ");
        Pstring = Pmulti_remain.GetSection().Add_string("type",Property.Changeable.WhenIdle,"dummy");
        Pmulti_remain.SetValue("dummy");
        Pstring.Set_values(serials);
        Pstring = Pmulti_remain.GetSection().Add_string("parameters",Property.Changeable.WhenIdle,"");
        Pmulti_remain.Set_help("see serial1");

        Pmulti_remain = secprop.Add_multiremain("serial3",Property.Changeable.WhenIdle," ");
        Pstring = Pmulti_remain.GetSection().Add_string("type",Property.Changeable.WhenIdle,"disabled");
        Pmulti_remain.SetValue("disabled");
        Pstring.Set_values(serials);
        Pstring = Pmulti_remain.GetSection().Add_string("parameters",Property.Changeable.WhenIdle,"");
        Pmulti_remain.Set_help("see serial1");

        Pmulti_remain = secprop.Add_multiremain("serial4",Property.Changeable.WhenIdle," ");
        Pstring = Pmulti_remain.GetSection().Add_string("type",Property.Changeable.WhenIdle,"disabled");
        Pmulti_remain.SetValue("disabled");
        Pstring.Set_values(serials);
        Pstring = Pmulti_remain.GetSection().Add_string("parameters",Property.Changeable.WhenIdle,"");
        Pmulti_remain.Set_help("see serial1");


        /* All the DOS Related stuff, which will eventually start up in the shell */
        secprop=control.AddSection_prop("dos", Dos.DOS_Init,false);//done
        secprop.AddInitFunction(XMS.XMS_Init,true);//done
        Pbool = secprop.Add_bool("xms",Property.Changeable.WhenIdle,true);
        Pbool.Set_help("Enable XMS support.");

        secprop.AddInitFunction(EMS.EMS_Init,true);//done
        String[] ems_settings = new String[]{ "true", "emsboard", "emm386", "false"};
	    Pstring = secprop.Add_string("ems",Property.Changeable.WhenIdle,"true");
	    Pstring.Set_values(ems_settings);
	    Pstring.Set_help("Enable EMS support. The default (=true) provides the best\n" +
		"compatibility but certain applications may run better with\n" +
		"other choices, or require EMS support to be disabled (=false)\n" +
		"to work at all.");

        Pbool = secprop.Add_bool("umb",Property.Changeable.WhenIdle,true);
        Pbool.Set_help("Enable UMB support.");

        secprop.AddInitFunction(DosKeyboardLayout.DOS_KeyboardLayout_Init,true);
        Pstring = secprop.Add_string("keyboardlayout",Property.Changeable.WhenIdle, "auto");
        Pstring.Set_help("Language code of the keyboard layout (or none).");

        // Mscdex
        secprop.AddInitFunction(DosMSCDEX.MSCDEX_Init);
        secprop.AddInitFunction(Drives.DRIVES_Init);
        secprop.AddInitFunction(CDRomImage.CDROM_Image_Init);
        if (Config.C_IPX) {
            secprop=control.AddSection_prop("ipx",IPX.IPX_Init,true);
            Pbool = secprop.Add_bool("ipx",Property.Changeable.WhenIdle, false);
            Pbool.Set_help("Enable ipx over UDP/IP emulation.");
        }
        
        /* IDE emulation options and setup */
        secprop=control.AddSection_prop("ide", IDE.IDE_Init,false);
        Pbool = secprop.Add_bool("primary",Property.Changeable.OnlyAtStart,true);
        Pbool.Set_help("Enable IDE interface for use with Bochs bios");
        Pbool = secprop.Add_bool("secondary",Property.Changeable.OnlyAtStart,true);
        Pbool.Set_help("Enable IDE interface for use with Bochs bios");
        Pbool = secprop.Add_bool("tertiary",Property.Changeable.OnlyAtStart,false);
        Pbool.Set_help("Enable IDE interface for use with Bochs bios");
        Pbool = secprop.Add_bool("quaternary",Property.Changeable.OnlyAtStart,false);
        Pbool.Set_help("Enable IDE interface for use with Bochs bios");

        secprop=control.AddSection_prop("floppy", Floppy.Flopyy_Init, true);
        Pbool = secprop.Add_bool("enable",Property.Changeable.OnlyAtStart,true);
        Pbool.Set_help("Enable Floppy controller for use with Bochs bios");

        if (Config.C_NE2000) {
            secprop=control.AddSection_prop("ne2000",NE2000.NE2000_Init,true);
            //Pstring = secprop.Add_string("ne2000",Property.Changeable.WhenIdle,"false");
            Msg.add("NE2000_CONFIGFILE_HELP",
                "macaddr -- The physical address the emulator will use on your network.\n" +
                "           If you have multiple DOSBoxes running on your network,\n" +
                "           this has to be changed. Modify the last three number blocks.\n" +
                "           I.e. AC:DE:48:88:99:AB.\n" +
                "realnic -- Specifies which of your network interfaces is used.\n" +
                "           Write \'list\' here to see the list of devices in the\n" +
                "           Status Window. Then make your choice and put either the\n" +
                "           interface number (2 or something) or a part of your adapters\n" +
                "           name, e.g. VIA here.\n"
            );

            String[] ne2000_settings = new String[]{ "false", "user", "pcap", "pcaphost"};
            Pstring = secprop.Add_string("mode", Property.Changeable.WhenIdle, "false");
            Pstring.Set_values(ne2000_settings);

            Pstring.Set_help( "none -- Hardware is not enabled.  This is the default.\n" +
                "user -- Internal router will be used.  Currently only DHCP works.\n" +
                "        UDP/TCP has not been implemented so the internet in\n" +
                "        Win98 won't work yet.\n" +
                "pcap -- This requires jnetpcap.jar, the appropriate native jnetpcap\n" +
                "        libaries and [Win]Pcap installed on the host computer.\n" +
                "        This is the best option if you want to browse network\n" +
                "        shares and use the internet on Win98\n" +
                "pcaphost -- This is not a reliable option yet and used for testing.\n");

            Pint = secprop.Add_int("pcapport", Property.Changeable.WhenIdle, 15654);
            Pint.Set_help("Used with mode=pcaphost. Port to connect to when forwarding pcap requests");
            Phex = secprop.Add_hex("nicbase", Property.Changeable.WhenIdle, new Hex(0x300));
            Phex.Set_help("The base address of the NE2000 board.");
            Pint = secprop.Add_int("nicirq", Property.Changeable.WhenIdle, 3);
            Pint.Set_help("The interrupt it uses. Note serial2 uses IRQ3 as default.");
            Pstring = secprop.Add_string("macaddr", Property.Changeable.WhenIdle,"AC:DE:48:88:99:AA");
            Pstring.Set_help("The physical address the emulator will use on your network.\n" +
                "If you have multiple DOSBoxes running on your network,\n" +
                "this has to be changed for each. AC:DE:48 is an address range reserved for\n" +
                "private use, so modify the last three number blocks.\n" +
                "I.e. AC:DE:48:88:99:AB.");
            Pstring = secprop.Add_string("realnic", Property.Changeable.WhenIdle,"list");
            Pstring.Set_help("Specifies which of your network interfaces is used.\n" +
                "Write \'list\' here to see the list of devices in the\n" +
                "Status Window. Then make your choice and put either the\n" +
                "interface number (2 or something) or a part of your adapters\n" +
                "name, e.g. VIA here.  This option is used for mode=\"pcap\"");
        }
        //	secprop.AddInitFunction(&CREDITS_Init);

        //TODO ?
        secline=control.AddSection_line("autoexec", Shell.AUTOEXEC_Init);
        Msg.add("AUTOEXEC_CONFIGFILE_HELP",
            "\n#Lines in this section will be run at startup.\n" +
            "#You can put your MOUNT lines here.\n"
        );
        Msg.add("CONFIGFILE_INTRO",
                "# This is the configuration file for DOSBox %s. (Please use the latest version of DOSBox)\n" +
                "# Lines starting with a # are comment lines and are ignored by DOSBox.\n" +
                "# They are used to (briefly) document the effect of each option.\n");
        Msg.add("CONFIG_SUGGESTED_VALUES", "Possible values");

        control.SetStartUp(Shell.SHELL_Init);
    }
}
