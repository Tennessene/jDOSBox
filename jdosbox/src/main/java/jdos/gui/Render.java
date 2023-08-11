package jdos.gui;

import jdos.hardware.Hardware;
import jdos.misc.Log;
import jdos.misc.setup.Section;
import jdos.misc.setup.Section_prop;
import jdos.sdl.JavaMapper;
import jdos.util.Ptr;

public class Render {
    // 0: complex scalers off, scaler cache off, some simple scalers off, memory requirements reduced
    // 1: complex scalers off, scaler cache off, all simple scalers on
    // 2: complex scalers off, scaler cache on
    // 3: complex scalers on
    static public final int RENDER_USE_ADVANCED_SCALERS = 0;

    static public final int RENDER_SKIP_CACHE = 16;
    //Enable this for scalers to support 0 input for empty lines
    //#define RENDER_NULL_INPUT

    public static class RenderPal_t {
        public RenderPal_t() {
            for (int i=0;i<rgb.length;i++)
                rgb[i] = new RGB();
        }
        public static class RGB {
            public /*Bit8u*/short red;
            public /*Bit8u*/short green;
            public /*Bit8u*/short blue;
            public /*Bit8u*/short unused;
        }
        RGB[] rgb = new RGB[256];

        //union {
        public static class LUT {
            public /*Bit16u*/short[] b16=new short[256];
            public /*Bit32u*/int[] b32=new int[256];
        }
        public LUT lut = new LUT();
        public boolean changed;
        public /*Bit8u*/Ptr modified = new Ptr(256);
        /*Bitu*/int first;
        /*Bitu*/int last;
    }

    public static class Render_t {
        public static class SRC {
            public /*Bitu*/int width, start;
            public /*Bitu*/int height;
            public /*Bitu*/int bpp;
            public boolean dblw,dblh;
            public double ratio;
            public float fps;
            public /*Bitu*/int outPitch;
            public /*Bit8u*/int[] outWrite32;
            public /*Bit8u*/short[] outWrite16;
            public /*Bit8u*/byte[] outWrite8;
            public int outWriteOff;
        }
        public SRC src = new SRC();
        public static class Frameskip {
            public /*Bitu*/int count;
            public /*Bitu*/int max;
            public /*Bitu*/int index;
            public boolean auto;
            public /*Bit8u*/boolean[] hadSkip = new boolean[RENDER_SKIP_CACHE];
        }
        public Frameskip frameskip = new Frameskip();
        public RenderPal_t pal=new RenderPal_t();
        public boolean updating;
        public boolean active;
        public boolean aspect;
        public boolean fullFrame;
        public int scale = 2;
        public boolean scaleForced = false;
    }

    static public Render_t render;

    static private void Check_Palette() {
        /* Clean up any previous changed palette data */
        if (render.pal.changed) {
            render.pal.modified.clear();
            render.pal.changed = false;
        }
        if (render.pal.first>render.pal.last)
            return;
        /*Bitu*/int i;
        switch (render.src.bpp) {
        case 8:
            Main.GFX_SetPalette(render.pal.first,render.pal.last-render.pal.first+1,render.pal.rgb, render.pal.first,Render.render.src.bpp);
            break;
        case 15:
        case 16:
            for (i=render.pal.first;i<=render.pal.last;i++) {
                /*Bit8u*/short r=render.pal.rgb[i].red;
                /*Bit8u*/short g=render.pal.rgb[i].green;
                /*Bit8u*/short b=render.pal.rgb[i].blue;
                /*Bit16u*/int newPal = Main.GFX_GetRGB(r,g,b);
                if (newPal != render.pal.lut.b16[i]) {
                    render.pal.changed = true;
                    render.pal.modified.set(i, 1);
                    render.pal.lut.b16[i]=(short)newPal;
                }
            }
            break;
        case 32:
        default:
            for (i=render.pal.first;i<=render.pal.last;i++) {
                /*Bit8u*/short r=render.pal.rgb[i].red;
                /*Bit8u*/short g=render.pal.rgb[i].green;
                /*Bit8u*/short b=render.pal.rgb[i].blue;
                /*Bit32u*/int newPal = Main.GFX_GetRGB(r,g,b);
                if (newPal != render.pal.lut.b32[i]) {
                    render.pal.changed = true;
                    render.pal.modified.set(i,1);
                    render.pal.lut.b32[i]=newPal;
                }
            }
            break;
        }
        /* Setup pal index to startup values */
        render.pal.first=256;
        render.pal.last=0;
    }

    static public void RENDER_SetPal(/*Bit8u*/int entry,/*Bit8u*/int red,/*Bit8u*/int green,/*Bit8u*/int blue) {
        render.pal.rgb[entry].red=(short)red;
        render.pal.rgb[entry].green=(short)green;
        render.pal.rgb[entry].blue=(short)blue;
        if (render.pal.first>entry) render.pal.first=entry;
        if (render.pal.last<entry) render.pal.last=entry;
    }

    public static boolean RENDER_StartUpdate() {
        if (render.updating)
            return false;
        if (!render.active)
            return false;
        if (render.frameskip.count<render.frameskip.max) {
            render.frameskip.count++;
            return false;
        }
        render.frameskip.count=0;
        if (render.src.bpp == 8) {
            Check_Palette();
        }

        Main.GFX_StartUpdate(render.src);
        render.src.outWriteOff = 0;

        /* Clearing the cache will first process the line to make sure it's never the same */
        render.fullFrame = true;
        render.updating = true;
        return true;
    }

    static private void RENDER_Halt() {
        Main.GFX_EndUpdate();
        render.updating=false;
        render.active=false;
    }

    static public void RENDER_EndUpdate( boolean abort ) {
        if (!render.updating)
            return;
        if ((Hardware.CaptureState & (Hardware.CAPTURE_IMAGE|Hardware.CAPTURE_VIDEO))!=0) {
            /*Bitu*/int pitch, flags;
            flags = 0;
            if (render.src.dblw != render.src.dblh) {
                if (render.src.dblw) flags|=Hardware.CAPTURE_FLAG_DBLW;
                if (render.src.dblh) flags|=Hardware.CAPTURE_FLAG_DBLH;
            }
            float fps = render.src.fps;
            pitch = render.src.outPitch;
            if (render.frameskip.max!=0)
                fps /= 1+render.frameskip.max;
            //Hardware.CAPTURE_AddImage( render.src.width, render.src.height, render.src.bpp, pitch, flags, fps, render.src.outWrite, render.pal.rgb );
        }
        if ( render.src.outWrite32 != null || render.src.outWrite16 != null || render.src.outWrite8 != null) {
            Main.GFX_EndUpdate();
            render.frameskip.hadSkip[render.frameskip.index] = false;
        } else {
//    #if 0
//            /*Bitu*/int total = 0, i;
//            render.frameskip.hadSkip[render.frameskip.index] = 1;
//            for (i = 0;i<RENDER_SKIP_CACHE;i++)
//                total += render.frameskip.hadSkip[i];
//            LOG_MSG( "Skipped frame %d %d", PIC_Ticks, (total * 100) / RENDER_SKIP_CACHE );
//    #endif
        }
        render.frameskip.index = (render.frameskip.index + 1) & (RENDER_SKIP_CACHE - 1);
        render.updating=false;
    }

    private static void RENDER_Reset() {
        /*Bitu*/int width=render.src.width;
        /*Bitu*/int height=render.src.height;

        if (render.src.dblh && render.src.dblw || (render.scaleForced && !render.src.dblh && !render.src.dblw)) {
            width*=render.scale;
            height*=render.scale;
        } else if (render.src.dblw) {
            width*=2;
        } else if (render.src.dblh) {
            height*=2;
        }
        Main.GFX_SetSize(width,height, render.src.width, render.src.height, Render.render.aspect,Render.render.src.bpp);

        /* Reset the palette change detection to it's initial value */
        render.pal.first= 0;
        render.pal.last = 255;
        render.pal.changed = false;
        render.pal.modified.clear();
        //Finish this frame using a copy only handler
        render.src.outWrite32 = null;
        render.src.outWrite16 = null;
        render.src.outWrite8 = null;
        /* Signal the next frame to first reinit the cache */
        render.active=true;
    }

    static private Main.GFX_CallBack_t RENDER_CallBack = new Main.GFX_CallBack_t() {
        public void call(int function) {
            if (function == Main.GFX_CallBackFunctions_t.GFX_CallBackStop) {
                RENDER_Halt( );
            } else if (function == Main.GFX_CallBackFunctions_t.GFX_CallBackRedraw) {
            } else if ( function == Main.GFX_CallBackFunctions_t.GFX_CallBackReset) {
                Main.GFX_EndUpdate();
                RENDER_Reset();
            } else {
                Log.exit("Unhandled GFX_CallBackReset "+function );
            }
        }
    };

    public static void RENDER_SetSize(/*Bitu*/int width,/*Bitu*/int height,/*Bitu*/int bpp,float fps,double ratio,boolean dblw,boolean dblh) {
        RENDER_Halt( );
        if (width==0 || height==0 ) {
            return;
        }
        if ( ratio > 1 ) {
            double target = height * ratio + 0.1;
            ratio = target / height;
        } else {
            //This would alter the width of the screen, we don't care about rounding errors here
        }
        render.src.width=width;
        render.src.height=height;
        render.src.bpp=bpp;
        render.src.dblw=dblw;
        render.src.dblh=dblh;
        render.src.fps=fps;
        render.src.ratio=ratio;
        RENDER_Reset( );
    }

    private static Mapper.MAPPER_Handler IncreaseFrameSkip = new Mapper.MAPPER_Handler() {
        public void call(boolean pressed) {
            if (!pressed)
                return;
            if (render.frameskip.max<10) render.frameskip.max++;
            Log.log_msg("Frame Skip at "+render.frameskip.max);
            Main.GFX_SetTitle(-1,render.frameskip.max,false);
        }
    };

    private static Mapper.MAPPER_Handler DecreaseFrameSkip = new Mapper.MAPPER_Handler() {
        public void call(boolean pressed) {
            if (!pressed)
                return;
            if (render.frameskip.max>0) render.frameskip.max--;
            Log.log_msg("Frame Skip at "+render.frameskip.max);
            Main.GFX_SetTitle(-1,render.frameskip.max,false);
        }
    };

    /* Disabled as I don't want to waste a keybind for that. Might be used in the future (Qbix)
    static void ChangeScaler(boolean pressed) {
        if (!pressed)
            return;
        render.scale.op = (scalerOperation)((int)render.scale.op+1);
        if((render.scale.op) >= scalerLast || render.scale.size == 1) {
            render.scale.op = (scalerOperation)0;
            if(++render.scale.size > 3)
                render.scale.size = 1;
        }
        RENDER_CallBack( GFX_CallBackReset );
    } */

    public static Section.SectionFunction RENDER_ShutDown = new Section.SectionFunction() {
        public void call(Section sec) {
            render = null;
            running = false;
        }
    };

    static boolean running = false;
    public static Section.SectionFunction RENDER_Init = new Section.SectionFunction() {
        public void call(Section sec) {
            Section_prop section=(Section_prop)sec;
            render = new Render_t();
            render.pal.first=256;
            render.pal.last=0;
            render.aspect=section.Get_bool("aspect");
            render.frameskip.max=section.Get_int("frameskip");
            if (render.frameskip.max<0) {
                render.frameskip.max = 0;
                render.frameskip.auto = true;
            }
            String scaler = section.Get_string("scaler").toLowerCase();
            if (scaler.startsWith("normal2x")) {
                render.scale = 2;
            } else if (scaler.startsWith("normal3x")) {
                render.scale = 3;
            } else {
                render.scale = 1;
            }
            if (scaler.contains("forced")) {
                render.scaleForced = true;
            }
            render.frameskip.count=0;

            if(!running) render.updating=true;
            running = true;

            JavaMapper.MAPPER_AddHandler(DecreaseFrameSkip, Mapper.MapKeys.MK_f7, Mapper.MMOD1, "decfskip", "Dec Fskip");
            JavaMapper.MAPPER_AddHandler(IncreaseFrameSkip, Mapper.MapKeys.MK_f8, Mapper.MMOD1, "incfskip", "Inc Fskip");
            Main.GFX_SetTitle(-1,render.frameskip.max,false);
            section.AddDestroyFunction(RENDER_ShutDown);
        }
    };
}
