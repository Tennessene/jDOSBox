package jdos.cpu.core_dynrec2;

import jdos.util.Ptr;

// decoding information used during translation of a code block
public final class DynDecode {
    public boolean modifiedAlot;
    /*PhysPt*/long code;			// pointer to next byte in the instruction stream
    /*PhysPt*/long code_start;		// pointer to the start of the current code block
    /*PhysPt*/long op_start;		// pointer to the start of the current instruction
    boolean big_op;			// operand modifier
    boolean big_addr;			// address modifier
    int rep;			// current repeat prefix
    /*Bitu*/int cycles;			// number cycles used by currently translated code
    boolean seg_prefix_used;	// segment overridden
    /*Bit8u*/int seg_prefix;		// segment prefix (if seg_prefix_used==true)

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

    // modrm state of the current instruction (if used)
    static public final class Modrm {
        /*Bitu*/int val;
        /*Bitu*/int mod;
        /*Bitu*/int rm;
        /*Bitu*/int reg;
    }
    final public Modrm modrm = new Modrm();
}
