package jdos.hardware;

import jdos.gui.Render;
import jdos.misc.setup.Module_base;
import jdos.misc.setup.Section;

public class Hardware extends Module_base {
    // OPL_Mode
	static public final int OPL_none = 0;
    static public final int OPL_cms = 1;
    static public final int OPL_opl2 = 2;
    static public final int OPL_dualopl2 = 3;
    static public final int OPL_opl3 = 4;

    static public final int CAPTURE_WAVE = 0x01;
    static public final int CAPTURE_OPL = 0x02;
    static public final int CAPTURE_MIDI = 0x04;
    static public final int CAPTURE_IMAGE = 0x08;
    static public final int CAPTURE_VIDEO = 0x10;

    static public final int CAPTURE_FLAG_DBLW = 0x1;
    static public final int CAPTURE_FLAG_DBLH = 0x2;

    static public int CaptureState = 0;

    static public void CAPTURE_AddImage(/*Bitu*/int width, /*Bitu*/int height, /*Bitu*/int bpp, /*Bitu*/int pitch, /*Bitu*/int flags, float fps, int[] data, Render.RenderPal_t.RGB[] pal) {
    }
    static public void CAPTURE_AddWave(/*Bit32u*/long freq, /*Bit32u*/long len, short[] data) {

    }
    public Hardware(Section configuration) {
        super(configuration);
    }
    
    public static Section.SectionFunction HARDWARE_Init = new Section.SectionFunction() {
        public void call(Section section) {
            System.out.println("HARDWARE_Init not finished yet: NO SCREEN SHOTS");
        }
    };
}
