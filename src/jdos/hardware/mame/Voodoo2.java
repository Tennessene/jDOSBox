package jdos.hardware.mame;

import jdos.misc.setup.Section;

public class Voodoo2 extends VoodooCommon {
    public Voodoo2(){
        super(0x0002, 4, 4, 4, TYPE_VOODOO_2);
    }

    private static Voodoo2 voodoo2;

    public static Section.SectionFunction Voodoo2_ShutDown = new Section.SectionFunction() {
        public void call(Section section) {
            voodoo2=null;
        }
    };

    public static Section.SectionFunction Voodoo2_Init = new Section.SectionFunction() {
        public void call(Section sec) {
            voodoo2 = new Voodoo2();
            sec.AddDestroyFunction(Voodoo2_ShutDown,false);
        }
    };
}
