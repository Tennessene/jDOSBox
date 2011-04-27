package jdos.cpu.core_dynamic;

import jdos.cpu.Core_dynrec;
import jdos.misc.Log;

public class Cache {
    public static class Block {
		public CacheBlockDynRec first;		// the first cache block in the list
		public CacheBlockDynRec active;		// the current cache block
		public CacheBlockDynRec free;		// pointer to the free list
		public CacheBlockDynRec running;		// the last block that was entered for execution
	}
    public Block block = new Block();
	/*Bit8u*/int pos;		// position in the cache block
	CodePageHandlerDynRec free_pages;		// pointer to the free list
	CodePageHandlerDynRec used_pages;		// pointer to the list of used pages
	CodePageHandlerDynRec last_page;		// the last used page

    public static Cache cache = new Cache();

    public static boolean cache_initialized = false;
    private static CacheBlockDynRec[] cache_blocks=null;
    
    public static void cache_init(boolean enable) {
        /*Bits*/int i;
        if (enable) {
            // see if cache is already initialized
            if (cache_initialized) return;
            cache_initialized = true;
            if (Cache.cache_blocks == null) {
                // allocate the cache blocks memory
                try {
                    cache_blocks=new CacheBlockDynRec[Core_dynrec.CACHE_BLOCKS];
                    for (i=0;i<cache_blocks.length;i++) {
                        cache_blocks[i] = new CacheBlockDynRec();
                    }
                } catch (Throwable e) {
                    Log.exit("Allocating cache_blocks has failed");
                }
                cache.block.free=cache_blocks[0];
                // initialize the cache blocks
                for (i=0;i<Core_dynrec.CACHE_BLOCKS-1;i++) {
                    //cache_blocks[i].link[0].to=(CacheBlockDynRec *)1;
                    //cache_blocks[i].link[1].to=(CacheBlockDynRec *)1;
                    cache_blocks[i].cache.next=cache_blocks[i+1];
                }
            }
//            if (cache_code_start_ptr==NULL) {
//                // allocate the code cache memory
//    #if defined (WIN32)
//                cache_code_start_ptr=(Bit8u*)VirtualAlloc(0,CACHE_TOTAL+CACHE_MAXSIZE+PAGESIZE_TEMP-1+PAGESIZE_TEMP,
//                    MEM_COMMIT,PAGE_EXECUTE_READWRITE);
//                if (!cache_code_start_ptr)
//                    cache_code_start_ptr=(Bit8u*)malloc(CACHE_TOTAL+CACHE_MAXSIZE+PAGESIZE_TEMP-1+PAGESIZE_TEMP);
//    #else
//                cache_code_start_ptr=(Bit8u*)malloc(CACHE_TOTAL+CACHE_MAXSIZE+PAGESIZE_TEMP-1+PAGESIZE_TEMP);
//    #endif
//                if(!cache_code_start_ptr) E_Exit("Allocating dynamic cache failed");
//
//                // align the cache at a page boundary
//                cache_code=(Bit8u*)(((long)cache_code_start_ptr + PAGESIZE_TEMP-1) & ~(PAGESIZE_TEMP-1)); //MEM LEAK. store old pointer if you want to free it.
//
//                cache_code_link_blocks=cache_code;
//                cache_code=cache_code+PAGESIZE_TEMP;
//
//    #if (C_HAVE_MPROTECT)
//                if(mprotect(cache_code_link_blocks,CACHE_TOTAL+CACHE_MAXSIZE+PAGESIZE_TEMP,PROT_WRITE|PROT_READ|PROT_EXEC))
//                    LOG_MSG("Setting excute permission on the code cache has failed");
//    #endif
            {
                CacheBlockDynRec block=cache_getblock();
                cache.block.first=block;
                cache.block.active=block;
                //block.cache.start=cache_code[0];
                block.cache.size=Core_dynrec.CACHE_TOTAL;
                block.cache.next=null;						// last block in the list
            }
//            // setup the default blocks for block linkage returns
//            cache.pos=&cache_code_link_blocks[0];
//            link_blocks[0].cache.start=cache.pos;
//            // link code that returns with a special return code
//            dyn_return(BR_Link1,false);
//            cache.pos=&cache_code_link_blocks[32];
//            link_blocks[1].cache.start=cache.pos;
//            // link code that returns with a special return code
//            dyn_return(BR_Link2,false);
//
//            cache.pos=&cache_code_link_blocks[64];
//            core_dynrec.runcode=(BlockReturn (*)(Bit8u*))cache.pos;
//    //		link_blocks[1].cache.start=cache.pos;
//            dyn_run_code();

            cache.free_pages=null;
            cache.last_page=null;
            cache.used_pages=null;
            // setup the code pages
            for (i=0;i<Core_dynrec.CACHE_PAGES;i++) {
                CodePageHandlerDynRec newpage=new CodePageHandlerDynRec();
                newpage.next=cache.free_pages;
                cache.free_pages=newpage;
            }
        }
    }
    static public void cache_addunusedblock(CacheBlockDynRec block) {
        // block has become unused, add it to the freelist
        block.cache.next=cache.block.free;
        cache.block.free=block;
    }

    public static CacheBlockDynRec cache_openblock() {
        CacheBlockDynRec block=cache.block.active;
        // check for enough space in this block
        /*Bitu*/int size=block.cache.size;
        CacheBlockDynRec nextblock=block.cache.next;
        if (block.page.handler!=null)
            block.Clear();
        // block size must be at least CACHE_MAXSIZE
        while (size< Core_dynrec.CACHE_MAXSIZE) {
            if (nextblock==null)
                break;
            // merge blocks
            size+=nextblock.cache.size;
            CacheBlockDynRec tempblock=nextblock.cache.next;
            if (nextblock.page.handler!=null)
                nextblock.Clear();
            // block is free now
            cache_addunusedblock(nextblock);
            nextblock=tempblock;
        }
        // adjust parameters and open this block
        block.cache.size=size;
        block.cache.next=nextblock;
        cache.pos=block.cache.start;
        return block;
    }

    public static CacheBlockDynRec cache_getblock() {
        // get a free cache block and advance the free pointer
        CacheBlockDynRec ret=cache.block.free;
        if (ret==null) Log.exit("Ran out of CacheBlocks" );
        cache.block.free=ret.cache.next;
        ret.cache.next=null;
        return ret;
    }

    public static void cache_closeblock() {
        CacheBlockDynRec block=cache.block.active;
        // links point to the default linking code
        block.link[0].to=CacheBlockDynRec.link_blocks[0];
        block.link[1].to=CacheBlockDynRec.link_blocks[1];
        block.link[0].from=null;
        block.link[1].from=null;
        block.link[0].next=null;
        block.link[1].next=null;
        // close the block with correct alignment
        /*Bitu*/int written=cache.pos-block.cache.start;
        if (written>block.cache.size) {
            if (block.cache.next==null) {
                if (written>block.cache.size+Core_dynrec.CACHE_MAXSIZE) Log.exit("CacheBlock overrun 1 "+(written-block.cache.size));
            } else Log.exit("CacheBlock overrun 2 written "+written+" size "+block.cache.size);
        } else {
            /*Bitu*/int new_size;
            /*Bitu*/int left=block.cache.size-written;
            // smaller than cache align then don't bother to resize
            if (left>Core_dynrec.CACHE_ALIGN) {
                new_size=((written-1)|(Core_dynrec.CACHE_ALIGN-1))+1;
                CacheBlockDynRec newblock=cache_getblock();
                // align block now to CACHE_ALIGN
                newblock.cache.start=block.cache.start+new_size;
                newblock.cache.size=block.cache.size-new_size;
                newblock.cache.next=block.cache.next;
                block.cache.next=newblock;
                block.cache.size=new_size;
            }
        }
        // advance the active block pointer
//        if (block.cache.next==null || (block.cache.next.cache.start>(cache_code_start_ptr + Core_dynrec.CACHE_TOTAL - Core_dynrec.CACHE_MAXSIZE))) {
//    //		LOG_MSG("Cache full restarting");
//            cache.block.active=cache.block.first;
//        } else {
            cache.block.active=block.cache.next;
//        }
    }
}
