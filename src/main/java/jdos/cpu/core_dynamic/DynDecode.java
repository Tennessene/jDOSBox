package jdos.cpu.core_dynamic;

import jdos.cpu.Paging;
import jdos.util.Ptr;

// decoding information used during translation of a code block
public final class DynDecode {
    public boolean modifiedAlot;
    public int code;			// pointer to next byte in the instruction stream
    public int code_start;		// pointer to the start of the current code block
    public int op_start;		// pointer to the start of the current instruction
    public int cycles;			// number cycles used by currently translated code
    public int tlb;

    // block that contains the first instruction translated
    public CacheBlockDynRec block;
    // block that contains the current byte of the instruction stream
    public CacheBlockDynRec active_block;

    // the active page (containing the current byte of the instruction stream)
    static public final class Page {
        public CodePageHandlerDynRec code;
        public int index;		// index to the current byte of the instruction stream
        public Ptr wmap;	// write map that indicates code presence for every byte of this page
        public Ptr invmap;	// invalidation map
        public int first;		// page number
    }
    public final Page page = new Page();

    public void setTLB(int address) {
        tlb = Paging.get_tlb_read(address);
        if (tlb == Paging.INVALID_ADDRESS) {
            Paging.get_tlb_readhandler(address).readb(address);
            tlb = Paging.get_tlb_read(address);
        }
    }
}
