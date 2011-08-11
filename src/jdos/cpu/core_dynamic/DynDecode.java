package jdos.cpu.core_dynamic;

import jdos.util.Ptr;

// decoding information used during translation of a code block
public final class DynDecode {
    public boolean modifiedAlot;
    /*PhysPt*/int code;			// pointer to next byte in the instruction stream
    /*PhysPt*/int code_start;		// pointer to the start of the current code block
    /*PhysPt*/int op_start;		// pointer to the start of the current instruction
    /*Bitu*/int cycles;			// number cycles used by currently translated code

    // block that contains the first instruction translated
    public CacheBlockDynRec block;
    // block that contains the current byte of the instruction stream
    public CacheBlockDynRec active_block;

    // the active page (containing the current byte of the instruction stream)
    static public final class Page {
        CodePageHandlerDynRec code;
        /*Bitu*/int index;		// index to the current byte of the instruction stream
        /*Bit8u*/ Ptr wmap;	// write map that indicates code presence for every byte of this page
        /*Bit8u*/Ptr invmap;	// invalidation map
        /*Bitu*/int first;		// page number
    }
    public final Page page = new Page();
}
