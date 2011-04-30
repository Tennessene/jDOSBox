package jdos.cpu.core_dynrec2;

import jdos.cpu.Paging;
import jdos.cpu.Core_dynrec2;
import jdos.util.ShortRef;
import jdos.util.IntRef;
import jdos.util.Ptr;
import jdos.misc.Log;
import jdos.hardware.Memory;

public class Decoder_basic {
    static public final int REP_NONE=0;
    static public final int REP_NZ=1;
    static public final int REP_Z=2;

    private static final ShortRef rdval = new ShortRef(0);
    private static final IntRef phys_page = new IntRef(0);

    static /*Bitu*/int mf_functions_num=0;
    static public final class _mf_functions {
        /*Bit8u*/ Ptr pos;
        //void* fct_ptr;
        /*Bitu*/int ftype;
    }
    static final _mf_functions[] mf_functions = new _mf_functions[64];

    static {
        for (int i=0;i<mf_functions.length;i++) {
            mf_functions[i] = new _mf_functions();
        }
    }
    public static void InitFlagsOptimization() {
        mf_functions_num=0;
    }
    public static boolean MakeCodePage(/*Bitu*/long lin_addr, Core_dynrec2.CodePageHandlerDynRecRef cph) {
        int i_line_addr = (int)lin_addr;

        //Ensure page contains memory:
        if (Paging.mem_readb_checked(lin_addr,rdval)) return true;

        Paging.PageHandler handler=Paging.get_tlb_readhandler(i_line_addr);
        if ((handler.flags & Paging.PFLAG_HASCODE)!=0) {
            // this is a codepage handler, and the one that we're looking for
            cph.value=(CodePageHandlerDynRec)handler;
            return false;
        }
        if ((handler.flags & Paging.PFLAG_NOCODE)!=0) {
            if (Paging.PAGING_ForcePageInit(lin_addr)) {
                handler=Paging.get_tlb_readhandler(i_line_addr);
                if ((handler.flags & Paging.PFLAG_HASCODE)!=0) {
                    cph.value=(CodePageHandlerDynRec)handler;
                    return false;
                }
            }
            if ((handler.flags & Paging.PFLAG_NOCODE)!=0) {
                Log.log_msg("DYNREC:Can't run code in this page");
                cph.value=null;
                return false;
            }
        }
        /*Bitu*/int lin_page=(int)(lin_addr>>12);
        phys_page.value=lin_page;
        // find the physical page that the linear page is mapped to
        if (!Paging.PAGING_MakePhysPage(phys_page)) {
            Log.log_msg("DYNREC:Can't find physpage");
            cph.value=null;
            return false;
        }
        // find a free CodePage
        if (Cache.cache.free_pages==null) {
            if (Cache.cache.used_pages!=Decoder.decode.page.code) Cache.cache.used_pages.ClearRelease();
            else {
                // try another page to avoid clearing our source-crosspage
                if ((Cache.cache.used_pages.next!=null) && (Cache.cache.used_pages.next!=Decoder.decode.page.code))
                    Cache.cache.used_pages.next.ClearRelease();
                else {
                    Log.log_msg("DYNREC:Invalid cache links");
                    Cache.cache.used_pages.ClearRelease();
                }
            }
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
        cph.value=cpagehandler;
        return false;
    }
}
