package jdos.cpu.core_dynamic;

import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Core_dynamic;
import jdos.cpu.Paging;
import jdos.hardware.Memory;
import jdos.hardware.RAM;
import jdos.util.Ptr;

final public class CodePageHandlerDynRec extends Paging.PageHandler {
    static public int activeCount = 0;
    static public int usedCount = 0;

	public CodePageHandlerDynRec() {
		invalidation_map=null;
	}

	public void SetupAt(/*Bitu*/int _phys_page, Paging.PageHandler _old_pagehandler) {
		// initialize this codepage handler
		phys_page=_phys_page;
		// save the old pagehandler to provide direct read access to the memory,
		// and to be able to restore it later on
		old_pagehandler=_old_pagehandler;

		// adjust flags
		flags=old_pagehandler.flags|Paging.PFLAG_HASCODE;
		flags&=~Paging.PFLAG_WRITEABLE;

		active_blocks=0;
		active_count=16;

		// initialize the maps with zero (no cache blocks as well as code present)
		invalidation_map=null;
	}

    // This will allow the current running instruction to finish, but when it moves
    // to the next one it will throw a null pointer exception which will then be
    // caught.
    private void invalidateRunningBlock() {
        Op op = Core_dynamic.currentBlock.code;
        while (op!=null) {
            Op next = op.next;
            op.next = null;
            op = next;
        }
    }

	// clear out blocks that contain code which has been modified
	void InvalidateRange(/*Bitu*/int start,/*Bitu*/int end) {
		/*Bits*/int index=1+(end>> Core_dynamic.DYN_HASH_SHIFT);
		boolean is_current_block=false;	// if the current block is modified, it has to be exited as soon as possible

		/*Bit32u*/int ip_point=(CPU_Regs.reg_csPhys.dword + CPU_Regs.reg_eip) & 0xFFF;
		while (index>=0) {
			/*Bitu*/int map=0;
			// see if there is still some code in the range
			for (/*Bitu*/int count=start;count<=end;count++) map+=write_map.p[count];
			if (map==0) { // no more code, finished
                if (is_current_block) {
                    DecodeBlock.smc = true;
                    invalidateRunningBlock();
                }
                return;
            }

			CacheBlockDynRec block=hash_map[index];
			while (block!=null) {
				CacheBlockDynRec nextblock=block.hash.next;
				// test if this block is in the range
				if (start<=block.page.end && end>=block.page.start) {
					if (block == Core_dynamic.currentBlock)
                        is_current_block=true;
					block.Clear();		// clear the block, decrements the write_map accordingly
				}
				block=nextblock;
			}
			index--;
		}
		if (is_current_block) {
            DecodeBlock.smc = true;
            invalidateRunningBlock();
        }
	}

	// the following functions will clean all cache blocks that are invalid now due to the write
	public void writeb(/*PhysPt*/int address,/*Bitu*/int val){
		int addr = (address & 4095);
		if (RAM.readb(hostmem + addr)==(val & 0xFF)) return;
		RAM.writeb(hostmem + addr, val);
		// see if there's code where we are writing to
		if (write_map.readb(addr)==0) {
			if (active_blocks!=0) return;		// still some blocks in this page
			active_count--;
			if (active_count==0) Release();	// delay page releasing until active_count is zero
			return;
		} else if (invalidation_map==null) {
			invalidation_map=new Ptr(4096);
		}
		invalidation_map.p[addr]++;
		InvalidateRange(addr,addr);
	}
	public void writew(/*PhysPt*/int address,/*Bitu*/int val){
		int addr = (address & 4095);
		if (RAM.readw(hostmem + addr)==(val & 0xFFFF)) return;
		RAM.writew(hostmem + addr, val);
		// see if there's code where we are writing to
		if (write_map.readw(addr)==0) {
			if (active_blocks!=0) return;		// still some blocks in this page
			active_count--;
			if (active_count==0) Release();	// delay page releasing until active_count is zero
			return;
		} else if (invalidation_map==null) {
			invalidation_map=new Ptr(4096);
		}
        invalidation_map.writew(addr, invalidation_map.readw(addr)+0x101);
		InvalidateRange(addr,addr+1);
	}
	public void writed(/*PhysPt*/int address,/*Bitu*/int val){
		int addr = (address & 4095);
		if (RAM.readd(hostmem + addr)==(val & 0xFFFFFFFF)) return;
		RAM.writed(hostmem + addr, val);
		// see if there's code where we are writing to
		if (write_map.readd(addr)==0) {
			if (active_blocks!=0) return;		// still some blocks in this page
			active_count--;
			if (active_count==0) Release();	// delay page releasing until active_count is zero
			return;
		} else if (invalidation_map==null) {
			invalidation_map=new Ptr(4096);
		}
        invalidation_map.writed(addr, invalidation_map.readd(addr)+0x1010101);
		InvalidateRange(addr,addr+3);
	}

    // add a cache block to this page and note it in the hash map
	public void AddCacheBlock(CacheBlockDynRec block) {
		/*Bitu*/int index=1+(block.page.start>> Core_dynamic.DYN_HASH_SHIFT);
		block.hash.next=hash_map[index];	// link to old block at index from the new block
		block.hash.index=index;
		hash_map[index]=block;				// put new block at hash position
		block.page.handler=this;
		active_blocks++;
        activeCount++;
        usedCount++;
        if ((usedCount % 1000)==0) {
            System.out.println("Dynamic code cache: "+activeCount+"/"+usedCount);
        }
	}
	// there's a block whose code started in a different page
    void AddCrossBlock(CacheBlockDynRec block) {
		block.hash.next=hash_map[0];
		block.hash.index=0;
		hash_map[0]=block;
		block.page.handler=this;
		active_blocks++;
	}
	// remove a cache block
	void DelCacheBlock(CacheBlockDynRec block) {
        activeCount--;
		active_blocks--;
		active_count=16;
        if (hash_map[block.hash.index]==block) {
            hash_map[block.hash.index] = block.hash.next;
        } else {
            CacheBlockDynRec parent = hash_map[block.hash.index];
            CacheBlockDynRec bwhere = parent.hash.next;
            while (bwhere != block) {
                parent = bwhere;
                bwhere = parent.hash.next;
            }
            parent.hash.next = block.hash.next;
        }
//		CacheBlockDynRec * * bwhere=&hash_map[block->hash.index];
//		while (*bwhere!=block) {
//			bwhere=&((*bwhere)->hash.next);
//			//Will crash if a block isn't found, which should never happen.
//		}
//		*bwhere=block->hash.next;

		// remove the cleared block from the write map
		if (block.cache.wmapmask!=null) {
			// first part is not influenced by the mask
			for (/*Bitu*/int i=block.page.start;i<block.cache.maskstart;i++) {
				if (write_map.p[i]!=0) write_map.p[i]--;
			}
			/*Bitu*/int maskct=0;
			// last part sticks to the writemap mask
			for (/*Bitu*/int i=block.cache.maskstart;i<=block.page.end;i++,maskct++) {
				if (write_map.p[i]!=0) {
					// only adjust writemap if it isn't masked
					if ((maskct>=block.cache.masklen) || (block.cache.wmapmask[maskct]==0)) write_map.p[i]--;
				}
			}
			block.cache.wmapmask=null;
		} else {
			for (/*Bitu*/int i=block.page.start;i<=block.page.end;i++) {
				if (write_map.p[i]!=0) write_map.p[i]--;
			}
		}
	}

	public void Release() {
		Memory.MEM_SetPageHandler(phys_page,1,old_pagehandler);	// revert to old handler
		Paging.PAGING_ClearTLB();

		// remove page from the lists
		if (prev!=null) prev.next=next;
		else Cache.cache.used_pages=next;
		if (next!=null) next.prev=prev;
		else Cache.cache.last_page=prev;
		next=Cache.cache.free_pages;
		Cache.cache.free_pages=this;
		prev=null;
	}

	public void ClearRelease() {
		// clear out all cache blocks in this page
		for (/*Bitu*/int index=0;index<(1+ Core_dynamic.DYN_PAGE_HASH);index++) {
			CacheBlockDynRec block=hash_map[index];
			while (block!=null) {
				CacheBlockDynRec nextblock=block.hash.next;
				block.Clear();
				block=nextblock;
			}
		}
		Release();	// now can release this page
	}

	public CacheBlockDynRec FindCacheBlock(/*Bitu*/int start) {
		CacheBlockDynRec block=hash_map[1+(start>> Core_dynamic.DYN_HASH_SHIFT)];
		// see if there's a cache block present at the start address
		while (block!=null) {
			if (block.page.start==start) return block;	// found
			block=block.hash.next;
		}
		return null;	// none found
	}

	public /*HostPt*/int GetHostReadPt(/*Bitu*/int phys_page) {
		hostmem=old_pagehandler.GetHostReadPt(phys_page);
		return hostmem;
	}

	public /*HostPt*/int GetHostWritePt(/*Bitu*/int phys_page) {
		return GetHostReadPt( phys_page );
	}

	// the write map, there are write_map[i] cache blocks that cover the byte at address i
	public /*Bit8u*/Ptr write_map=new Ptr(4096);
	public /*Bit8u*/ Ptr invalidation_map;
	CodePageHandlerDynRec next, prev;	// page linking

	private Paging.PageHandler old_pagehandler;

	// hash map to quickly find the cache blocks in this page
	private CacheBlockDynRec[] hash_map = new CacheBlockDynRec[1+ Core_dynamic.DYN_PAGE_HASH];

	private /*Bitu*/int active_blocks;		// the number of cache blocks in this page
	private /*Bitu*/int active_count;		// delaying parameter to not immediately release a page
	private /*HostPt*/int hostmem;
	private /*Bitu*/int phys_page;
}
