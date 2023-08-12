package jdos.gui;

public class Mapper {
    public static final int MMOD1 = 0x1;
    public static final int MMOD2 = 0x2;

    public static interface MAPPER_Handler {
        public void call(boolean pressed);
    }

    public static final class MapKeys {
	    public static final int MK_f1=0;
        public static final int MK_f2=1;
        public static final int MK_f3=2;
        public static final int MK_f4=3;
        public static final int MK_f5=4;
        public static final int MK_f6=5;
        public static final int MK_f7=6;
        public static final int MK_f8=7;
        public static final int MK_f9=8;
        public static final int MK_f10=9;
        public static final int MK_f11=10;
        public static final int MK_f12=11;
	    public static final int MK_return=12;
        public static final int MK_kpminus=13;
        public static final int MK_scrolllock=14;
        public static final int MK_printscreen=15;
        public static final int MK_pause=16;
        public static final int MK_home=17;
    }

}
