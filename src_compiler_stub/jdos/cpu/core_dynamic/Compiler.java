package jdos.cpu.core_dynamic;

import jdos.misc.setup.Section;

public class Compiler {
    static public boolean saveClasses = false;
    static public int min_block_size = 1;
    static public boolean alwayUseFastVersion = true;
    static public int processorCount = 0;
    static public boolean thowException = false;
    static public final boolean ENABLED = false;

    public static void compile(DecodeBlock op) {
    }
    static public void removeFromQueue(DecodeBlock block) {
    }
    final public static Section.SectionFunction Compiler_Init = new Section.SectionFunction() {
        public void call(Section newconfig) {
        }
    };
}
