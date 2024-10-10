package jdos.cpu.core_dynamic;

public class Cache {
    public static class Block {
		public CacheBlockDynRec first;		// the first cache block in the list
		public CacheBlockDynRec active;		// the current cache block
		public CacheBlockDynRec free;		// pointer to the free list
	}
    public Block block = new Block();
	/*Bit8u*/int pos;		// position in the cache block
	CodePageHandlerDynRec free_pages;		// pointer to the free list
	CodePageHandlerDynRec used_pages;		// pointer to the list of used pages
	CodePageHandlerDynRec last_page;		// the last used page

    public static Cache cache = new Cache();

    public static boolean cache_initialized = false;
    
    public static void cache_init(boolean enable) {
        /*Bits*/int i;
        if (enable) {
            // see if cache is already initialized
            if (cache_initialized) return;
            cache_initialized = true;

            {
                CacheBlockDynRec block=cache_getblock();
                cache.block.first=block;
                cache.block.active=block;
                //block.cache.start=cache_code[0];
                block.cache.next=null;						// last block in the list
            }

            cache.free_pages=null;
            cache.last_page=null;
            cache.used_pages=null;
        }
    }
    static public void cache_addunusedblock(CacheBlockDynRec block) {
        // block has become unused, add it to the freelist
        block.cache.next=cache.block.free;
        cache.block.free=block;
    }

    public static CacheBlockDynRec cache_openblock() {
        CacheBlockDynRec block=cache.block.active;
        CacheBlockDynRec nextblock=block.cache.next;
        if (block.page.handler!=null)
            block.Clear();
        // adjust parameters and open this block
        block.cache.next=nextblock;
        return block;
    }

    public static CacheBlockDynRec cache_getblock() {
        // get a free cache block and advance the free pointer
        CacheBlockDynRec ret=cache.block.free;
        if (ret==null) ret = new CacheBlockDynRec();
        cache.block.free=ret.cache.next;
        ret.cache.next=null;
        return ret;
    }

    public static void cache_closeblock() {
        CacheBlockDynRec block=cache.block.active;
        // links point to the default linking code
        block.link[0].to=null;
        block.link[1].to=null;
        block.link[0].from=null;
        block.link[1].from=null;

        if (block.cache.next == null) {
            CacheBlockDynRec newblock=cache_getblock();
            newblock.cache.next=block.cache.next;
            block.cache.next=newblock;
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
