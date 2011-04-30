package jdos.cpu.core_dynrec2;

import jdos.Dosbox;
import jdos.cpu.Core_dynrec2;
import jdos.hardware.Memory;
import jdos.misc.Log;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    private static CacheBlockDynRec[] cache_blocks=null;

    public static void cache_init(boolean enable) {
        /*Bits*/int i;
        if (enable) {
            // see if cache is already initialized
            if (cache_initialized) return;
            cache_load_index();
            cache_initialized = true;
            if (Cache.cache_blocks == null) {
                // allocate the cache blocks memory
                try {
                    cache_blocks=new CacheBlockDynRec[Core_dynrec2.CACHE_BLOCKS];
                    for (i=0;i<cache_blocks.length;i++) {
                        cache_blocks[i] = new CacheBlockDynRec();
                    }
                } catch (Throwable e) {
                    Log.exit("Allocating cache_blocks has failed");
                }
                cache.block.free=cache_blocks[0];
                // initialize the cache blocks
                for (i=0;i< Core_dynrec2.CACHE_BLOCKS-1;i++) {
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
//            core_dynrec2.runcode=(BlockReturn (*)(Bit8u*))cache.pos;
//    //		link_blocks[1].cache.start=cache.pos;
//            dyn_run_code();

            cache.free_pages=null;
            cache.last_page=null;
            cache.used_pages=null;
            // setup the code pages
            for (i=0;i< Core_dynrec2.CACHE_PAGES;i++) {
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
        if (ret==null) Log.exit("Ran out of CacheBlocks" );
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

    static Vector instruction_cache = new Vector();
    private static class Instructions {
        byte[] byteCode;
        String name;
        String md5;
    }
    public static void cache_save(String name, String md5, byte[] byteCode) {
        Instructions i = new Instructions();
        i.byteCode = byteCode;
        i.md5 = md5;
        i.name = name;
        instruction_cache.add(i);
    }

    static Hashtable instruction_index = new Hashtable();
    static int countHit;
    public static DynamicClass getCode(String md5) {
        String name = (String)instruction_index.get(md5);
        if (name!=null) {
            try {
                return (DynamicClass)Class.forName(name).newInstance();
            } catch (Exception e) {
            }
        }
        return null;
    }
    public static void cache_load_index() {
        InputStream is = Dosbox.class.getResourceAsStream("Cache.index");
        if (is != null) {
            DataInputStream dis = new DataInputStream(is);
            try {
                int count = dis.readInt();
                for (int i=0;i<count;i++) {
                    String name = dis.readUTF();
                    String md5 = dis.readUTF();
                    instruction_index.put(md5, name);
                }
                jdos.cpu.core_dynrec2.Decoder.count = count;
            } catch (Exception e) {
            }
        }

    }
    public static void cache_write(String fileName) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeInt(instruction_cache.size());
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
            for (int i=0;i<instruction_cache.size();i++) {
                Instructions inst = (Instructions)instruction_cache.elementAt(i);
                out.putNextEntry(new ZipEntry(inst.name + ".class"));
                out.write(inst.byteCode);
                dos.writeUTF(inst.name);
                dos.writeUTF(inst.md5);
            }
            out.putNextEntry(new ZipEntry("jdos/Cache.index"));
            dos.flush();
            out.write(bos.toByteArray());
            out.flush();
            out.close();
            System.out.println("Cache: Saved "+instruction_cache.size()+" methods");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
