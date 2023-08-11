package jdos.win.loader.winpe;

import java.io.IOException;

public class HeaderImageImportDescriptor {
    static public final int SIZE = 20;

    /* 0 for terminating null import descriptor  */
    /* RVA to original unbound IAT */
    public long Characteristics_or_OriginalFirstThunk;

    public long TimeDateStamp;     /* 0 if not bound,
                                    * -1 if bound, and real date\time stamp
                                    *    in IMAGE_DIRECTORY_ENTRY_BOUND_IMPORT
                                    * (new BIND)
                                    * otherwise date/time stamp of DLL bound to
                                    * (Old BIND)
                                    */
    public long ForwarderChain;    /* -1 if no forwarders */
    public long Name;
    /* RVA to IAT (if bound this IAT has actual addresses) */
    public long FirstThunk;

    public void load(LittleEndianFile is) throws IOException {
        Characteristics_or_OriginalFirstThunk = is.readUnsignedInt();
        TimeDateStamp = is.readUnsignedInt();
        ForwarderChain = is.readUnsignedInt();
        Name = is.readUnsignedInt();
        FirstThunk = is.readUnsignedInt();
    }
}
