package jdos.cpu.core_dynamic;

public class CacheBlockDynRec {
    static CacheBlockDynRec[] link_blocks=new CacheBlockDynRec[2];		// default linking (specially marked)

    static {
        for (int i=0;i<link_blocks.length;i++) {
            link_blocks[i] = new CacheBlockDynRec();
        }
    }
    public CacheBlockDynRec() {
        for (int i=0;i<link.length;i++)
            link[i] = new _Link();
    }
	public void Clear() {
        /*Bitu*/int ind;
        // check if this is not a cross page block
        if (hash.index!=0) {
            for (ind=0;ind<2;ind++) {
                CacheBlockDynRec fromlink=link[ind].from;
                link[ind].from=null;
                while (fromlink!=null) {
                    CacheBlockDynRec nextlink=fromlink.link[ind].next;
                    // clear the next-link and let the block point to the standard linkcode
                    fromlink.link[ind].next=null;
                    fromlink.link[ind].to=link_blocks[ind];

                    fromlink=nextlink;
                }
                if (link[ind].to!=link_blocks[ind]) {
                    // not linked to the standard linkcode, find the block that links to this block
                    if (link[ind].to.link[ind].from == this) {
                        link[ind].to.link[ind].from = link[ind].to.link[ind].from.link[ind].next;
                    } else {
                        CacheBlockDynRec parent = link[ind].to.link[ind].from;
                        CacheBlockDynRec wherelink = parent.link[ind].next;
                        while (wherelink!=this) {
                            parent = wherelink;
                            wherelink = parent.link[ind].next;
                        }
                        parent.link[ind].next = wherelink.link[ind].next;
                    }
//                    CacheBlockDynRec * * wherelink=&link[ind].to->link[ind].from;
//                    while (*wherelink != this && *wherelink) {
//                        wherelink = &(*wherelink)->link[ind].next;
//                    }
//                    // now remove the link
//                    if(*wherelink)
//                        *wherelink = (*wherelink)->link[ind].next;
//                    else {
//                        LOG(LOG_CPU,LOG_ERROR)("Cache anomaly. please investigate");
//                    }
                }
            }
        } else
            Cache.cache_addunusedblock(this);
        if (crossblock!=null) {
            // clear out the crossblock (in the page before) as well
            crossblock.crossblock=null;
            crossblock.Clear();
            crossblock=null;
        }
        if (page.handler!=null) {
            // clear out the code page handler
            page.handler.DelCacheBlock(this);
            page.handler=null;
        }
        cache.wmapmask=null;
    }
	// link this cache block to another block, index specifies the code
	// path (always zero for unconditional links, 0/1 for conditional ones
	public void LinkTo(/*Bitu*/int index, CacheBlockDynRec toblock) {
		if (toblock == null) throw new NullPointerException();
		link[index].to=toblock;
		link[index].next=toblock.link[index].from;	// set target block
		toblock.link[index].from=this;				// remember who links me
	}
	public class Page {
		/*Bit16u*/int start,end;		// where in the page is the original code
		CodePageHandlerDynRec  handler;			// page containing this code
	}
    public Page page = new Page();

	static public class _Cache {
		public /*Bit8u * */int start;			// where in the cache are we
		public/*Bitu*/int size;
		public CacheBlockDynRec next;
		// writemap masking maskpointer/start/length
		// to allow holes in the writemap
		public /*Bit8u*/byte[] wmapmask;
		public /*Bit16u*/int maskstart;
		public /*Bit16u*/int masklen;
	}
    public _Cache cache = new _Cache();

	static public class _Hash {
		/*Bitu*/int index;
		CacheBlockDynRec next;
	}
    public _Hash hash = new _Hash();
	static public class _Link {
		CacheBlockDynRec to;		// this block can transfer control to the to-block
		CacheBlockDynRec next;
		CacheBlockDynRec from;	// the from-block can transfer control to this block
	}
    public _Link[] link = new _Link[2];
	CacheBlockDynRec crossblock;
    public DynamicClass code;
}

