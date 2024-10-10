package jdos.cpu.core_dynamic;

import jdos.cpu.Paging;
import jdos.hardware.Memory;
import jdos.misc.Log;
import jdos.util.IntRef;

public class Decoder_basic {
    private static final IntRef phys_page = new IntRef(0);

    public static CodePageHandlerDynRec MakeCodePage(/*Bitu*/int lin_addr) {
        Memory.mem_readb(lin_addr); // generate page fault here if necessary

        Paging.PageHandler handler=Paging.get_tlb_readhandler(lin_addr);
        if ((handler.flags & Paging.PFLAG_HASCODE)!=0) {
            // this is a codepage handler, and the one that we're looking for
            return (CodePageHandlerDynRec)handler;
        }
        if ((handler.flags & Paging.PFLAG_NOCODE)!=0) {
            //Log.log_msg("DYNREC:Can't run code in this page");
            return null;
        }
        /*Bitu*/int lin_page=lin_addr>>>12;
        phys_page.value=lin_page;
        // find the physical page that the linear page is mapped to
        if (!Paging.PAGING_MakePhysPage(phys_page)) {
            Log.log_msg("DYNREC:Can't find physpage for lin addr "+Integer.toString(lin_addr, 16));
            return null;
        }
        // find a free CodePage
        if (Cache.cache.free_pages==null) {
            Cache.cache.free_pages = new CodePageHandlerDynRec();
        }
        CodePageHandlerDynRec cpagehandler=Cache.cache.free_pages;
        Cache.cache.free_pages=Cache.cache.free_pages.next;

        // adjust previous and next page pointer
        cpagehandler.prev=Cache.cache.last_page;
        cpagehandler.next=null;
        if (Cache.cache.last_page!=null) Cache.cache.last_page.next=cpagehandler;
        Cache.cache.last_page=cpagehandler;
        if (Cache.cache.used_pages==null) Cache.cache.used_pages=cpagehandler;

        // initialize the code page handler and add the handler to the memory page
        cpagehandler.SetupAt(phys_page.value,handler);
        Memory.MEM_SetPageHandler(phys_page.value,1,cpagehandler);
        Paging.PAGING_UnlinkPages(lin_page,1);
        return cpagehandler;
    }
}
