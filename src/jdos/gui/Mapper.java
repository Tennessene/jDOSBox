package jdos.gui;

public class Mapper {
    public static final int MMOD1 = 0x1;
    public static final int MMOD2 = 0x2;

    public static interface MAPPER_Handler {
        public void call(boolean pressed);
    }

    public static enum MapKeys {
	    MK_f1,MK_f2,MK_f3,MK_f4,MK_f5,MK_f6,MK_f7,MK_f8,MK_f9,MK_f10,MK_f11,MK_f12,
	    MK_return,MK_kpminus,MK_scrolllock,MK_printscreen,MK_pause
    }

    public static void MAPPER_AddHandler(MAPPER_Handler handler,MapKeys key,/*Bitu*/int mods,String eventname,String buttonname) {

    }
}
