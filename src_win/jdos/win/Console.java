package jdos.win;

import jdos.dos.Dos_files;
import jdos.util.IntRef;

public class Console {
    static public void out(String msg) {
        byte[] out = msg.getBytes();
        IntRef s = new IntRef(out.length);
        Dos_files.DOS_WriteFile(Dos_files.STDOUT, out, s);
    }
}
