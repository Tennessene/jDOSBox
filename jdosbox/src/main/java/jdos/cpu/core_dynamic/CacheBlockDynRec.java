package jdos.cpu.core_dynamic;

import jdos.Dosbox;
import jdos.cpu.core_switch.SwitchBlock;
import jdos.misc.Log;

import java.util.Vector;

public class CacheBlockDynRec {
    public CacheBlockDynRec() {
        for (int i=0;i<link.length;i++)
            link[i] = new _Link();
        link1 = link[0];
        link2 = link[1];
        this.inst = null;
    }

	public void Clear() {
        /*Bitu*/int ind;
        if (code instanceof DecodeBlock && Dosbox.allPrivileges) {
            DecodeBlock op = (DecodeBlock)code;
            Compiler.removeFromQueue(op);
        }
        // check if this is not a cross page block
        if (hash.index!=0) {
            for (ind=0;ind<2;ind++) {
                Vector fromlink=link[ind].from;
                if (link[ind].from != null) {
                    for (int i=0;i<link[ind].from.size();i++) {
                        CacheBlockDynRec from = (CacheBlockDynRec)link[ind].from.elementAt(i);
                        if (from.link[ind].to != this) {
                            //Log.exit("Bad Dynamic cache");
                        }
                        from.link[ind].to = null;
                    }
                    link[ind].from = null;
                }
                if (link[ind].to!=null && link[ind].to!=this) {
                    link[ind].to.link[ind].from.remove(this);
                    if (link[ind].to.link[ind].from.size()==0) {
                        link[ind].to.link[ind].from = null;
                    }
                    link[ind].to = null;
                }
            }
        }
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
        if (link[index].to != null) {
            Log.exit("Dynamic cache failure");
        }
		link[index].to=toblock;
        if (toblock.link[index].from == null)
            toblock.link[index].from = new Vector();
		toblock.link[index].from.add(this);				// remember who links me
	}
	public class Page {
		public int start,end;		// where in the page is the original code
		public CodePageHandlerDynRec  handler;			// page containing this code
	}
    public Page page = new Page();

	static public class _Cache {
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
		public CacheBlockDynRec to;		// this block can transfer control to the to-block
		public Vector from = new Vector();	// the from-block can transfer control to this block
	}
    public _Link[] link = new _Link[2];
    public _Link link1;
    public _Link link2;
	CacheBlockDynRec crossblock;
    public Op code;
    public SwitchBlock[] inst; // micro instructions used by Core_switch
    public byte[] originalByteCode = null; //used for dynamic core cache verification
}

