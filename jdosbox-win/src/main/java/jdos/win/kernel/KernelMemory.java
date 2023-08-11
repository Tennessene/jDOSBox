package jdos.win.kernel;

import jdos.cpu.CPU;
import jdos.cpu.Callback;
import jdos.hardware.Memory;
import jdos.util.IntRef;
import jdos.win.Win;
import jdos.win.builtin.kernel32.WinProcess;

// Based heavily on James Molloy's work at http://www.jamesmolloy.co.uk/tutorial_html/6.-Paging.html

public class KernelMemory {
    static final private int PLACEMENT_START = 0x100000; // map the first meg for dos  :TODO: research not wasting 1MB per process
    int placement_address = PLACEMENT_START;
    KernelHeap heap = null;
    static private final int KHEAP_INITIAL_SIZE = 0x10000;
    private long KHEAP_START = WinProcess.ADDRESS_KHEAP_START;
    private long KHEAP_END = WinProcess.ADDRESS_KHEAP_END;

    public int kmalloc(int sz) {
        return kmalloc(sz, false, null);
    }

    public void kfree(int p) {
        heap.free(p);
    }

    public int kmalloc(int sz, boolean align, IntRef phys) {
        if (heap != null) {
            int addr = heap.alloc(sz, align);
            if (phys != null) {
                int pageAddress = get_page(addr, false, kernel_directory);
                int page = Memory.mem_readd(pageAddress);
                phys.value = (page >>> 12) * 0x1000 + (addr & 0xFFF);
            }
            return addr;
        } else {
            if (align && (placement_address & 0xFFF) != 0) { // If the address is not already page-aligned
                // Align it.
                placement_address &= 0xFFFFF000;
                placement_address += 0x1000;
            }
            if (phys != null) {
                phys.value = placement_address;
            }
            int tmp = placement_address;
            placement_address += sz;
            return tmp;
        }
    }

    static class Page {
        private static final int PRESENT_MASK = 0x01; // Page present in memory
        private static final int RW_MASK = 0x02; // Read-only if clear, readwrite if set
        private static final int USER_MASK = 0x04; // Supervisor level only if clear
        private static final int ACCESSED_MASK = 0x08; // Has the page been accessed since last refresh?
        private static final int DIRTY_MASK = 0x10; // // Has the page been written to since last refresh?

        static public int set(int page, boolean b, int mask) {
            page &= ~mask;
            if (b)
                page |= mask;
            return page;
        }

        static public boolean get(int page, int mask) {
            return (page & mask) != 0;
        }

        static public int getFrame(int page) {
            return page >>> 12;
        }

        static public int setFrame(int page, int frame) {
            page |= frame << 12;
            return page;
        }
    }

    static class PageDirectory {
        public static final int PAGE_TABLE_COUNT = 1024;
        public static final int TABLES_ENTRY_SIZE = 4;
        public static final int TABLES_PHYSICAL_SIZE = 4;
        public static final int SIZE = 4096 + 4096 + 4;
        public static final int TABLES_OFFSET = 0;
        public static final int TABLES_PHYSICAL_OFFSET = 4096;

        /*
           Array of pointers to pagetables.
        */
        public int[][] tables = new int[1024][];

        /*
            Array of pointers to the pagetables above, but gives their *physical*
            location, for loading into the CR3 register.
        */
        public int[] tablesPhysical = new int[1024];

        /*
            The physical address of tablesPhysical. This comes into play
            when we get our kernel heap allocated and the directory
            may be in a different location in virtual memory.
        */
        public int physicalAddr;
    }

    int[] frames = null;
    int nframes = 0;
    int kernel_directory = 0;
    int current_directory = 0;

    static private int INDEX_FROM_BIT(int a) {
        return a / (8 * 4);
    }

    static private int OFFSET_FROM_BIT(int a) {
        return a % (8 * 4);
    }

    private void set_frame(int frame) {
        int idx = INDEX_FROM_BIT(frame);
        int off = OFFSET_FROM_BIT(frame);
        frames[idx] |= (0x1 << off);
    }

    // Static function to clear a bit in the frames bitset
    private void clear_frame(int frame) {
        int idx = INDEX_FROM_BIT(frame);
        int off = OFFSET_FROM_BIT(frame);
        frames[idx] &= ~(0x1 << off);
    }

    // Static function to test if a bit is set.
    private int test_frame(int frame_addr) {
        int frame = frame_addr / 0x1000;
        int idx = INDEX_FROM_BIT(frame);
        int off = OFFSET_FROM_BIT(frame);
        return (frames[idx] & (0x1 << off));
    }

    // Static function to find the first free frame.
    private int first_frame() {
        int i, j;
        for (i = 0; i < INDEX_FROM_BIT(nframes); i++) {
            if (frames[i] != 0xFFFFFFFF) { // nothing free, exit early.
                // at least one bit is free here.
                for (j = 0; j < 32; j++) {
                    int toTest = 0x1 << j;
                    if ((frames[i] & toTest) == 0) {
                        return i * 4 * 8 + j;
                    }
                }
            }
        }
        return -1;
    }

    public void getInfo(IntRef free, IntRef used) {
        free.value = 0;
        used.value = 0;
        for (int i = 0; i < INDEX_FROM_BIT(nframes); i++) {
            for (int j = 0; j < 32; j++) {
                int toTest = 0x1 << j;
                if ((frames[i] & toTest) == 0) {
                    free.value++;
                } else {
                    used.value++;
                }
            }
        }
    }

    public void printInfo() {
        IntRef free = new IntRef(0);
        IntRef used = new IntRef(0);

        getInfo(free, used);
        System.out.print((used.value*4)+"/"+((used.value+free.value)*4)+"KB");
    }
    public int getNextFrame() {
        int frame = first_frame();
        if (frame == -1) {
            Win.panic("No free frames!");
        }
        set_frame(frame);
        return frame;
    }

    public void freeFrame(int frame) {
        clear_frame(frame);
    }

    static public void clearPage(int pagePtr) {
        Memory.mem_writed(pagePtr,0);
    }

    static public void setPage(int pagePtr, int frame, boolean is_kernel, boolean is_writeable) {
        int page = Memory.mem_readd(pagePtr);
        page = Page.set(page, true, Page.PRESENT_MASK); // Mark it as present.
        page = Page.set(page, is_writeable, Page.RW_MASK); // Should the page be writeable?
        page = Page.set(page, !is_kernel, Page.RW_MASK); // Should the page be user-mode?
        page = Page.setFrame(page, frame);
        Memory.mem_writed(pagePtr, page);
    }

    // Function to allocate a frame.
    void alloc_frame(int pagePtr, boolean is_kernel, boolean is_writeable) {
        int page = Memory.mem_readd(pagePtr);
        if (Page.getFrame(page) != 0) {
            return; // Frame was already allocated, return straight away.  This will only happen during initialization of paging
        } else {
            int idx = first_frame(); // idx is now the index of the first free frame.
            if (idx == -1) {
                Win.panic("No free frames!");
            }
            set_frame(idx); // this frame is now ours!
            page = Page.set(page, true, Page.PRESENT_MASK); // Mark it as present.
            page = Page.set(page, is_writeable, Page.RW_MASK); // Should the page be writeable?
            page = Page.set(page, !is_kernel, Page.RW_MASK); // Should the page be user-mode?
            page = Page.setFrame(page, idx);
            Memory.mem_writed(pagePtr, page);
        }
    }

    // Function to deallocate a frame.
    public void free_frame(int pagePtr) {
        int page = Memory.mem_readd(pagePtr);
        int frame = Page.getFrame(page);
        if (frame != 0) {
            clear_frame(frame); // Frame is now free again.
            page = Page.setFrame(page, 0); // Page now doesn't have a frame.
            Memory.mem_writed(pagePtr, page);
        }
    }

    public int createNewDirectory() {
        IntRef result = new IntRef(0);
        int vresult = kmalloc(PageDirectory.SIZE, true, result);
        Memory.mem_zero(vresult, PageDirectory.SIZE);
        for (int i=0;i<1024;i++) {
            int tablePtr = Memory.phys_readd(kernel_directory + PageDirectory.TABLES_OFFSET + i * PageDirectory.TABLES_ENTRY_SIZE);
            if (tablePtr != 0) {
                int physicalPtr = Memory.phys_readd(kernel_directory + PageDirectory.TABLES_PHYSICAL_OFFSET + i * PageDirectory.TABLES_PHYSICAL_SIZE);
                Memory.phys_writed(result.value + PageDirectory.TABLES_OFFSET + i * PageDirectory.TABLES_ENTRY_SIZE, tablePtr);
                Memory.phys_writed(result.value + PageDirectory.TABLES_PHYSICAL_OFFSET + i * PageDirectory.TABLES_PHYSICAL_SIZE, physicalPtr);
            }
        }
        return result.value;
    }

    public void initialise_paging() {
        // The size of physical memory.
        int mem_end_page = Memory.MEM_SIZE * 1024 * 1024;

        nframes = mem_end_page / 0x1000;
        frames = new int[INDEX_FROM_BIT(nframes)];

        // Let's make a page directory.
        kernel_directory = kmalloc(PageDirectory.SIZE, true, null);
        Memory.mem_zero(kernel_directory, PageDirectory.SIZE);
        current_directory = kernel_directory;

        // We need to identity map (phys addr = virt addr) from
        // 0x0 to the end of used memory, so we can access this
        // transparently, as if paging wasn't enabled.
        // NOTE that we use a while loop here deliberately.
        // inside the loop body we actually change placement_address
        // by calling kmalloc(). A while loop causes this to be
        // computed on-the-fly rather than once at the start.
        long i = 0;
        while (i < placement_address+0x1000) {
            // Kernel code is readable but not writeable from userspace.
            alloc_frame(get_page((int)i, true, kernel_directory), false, false);
            i += 0x1000;
        }
        int oldPlacement = placement_address;
        for (i = KHEAP_START;i<KHEAP_START+KHEAP_INITIAL_SIZE;i+=0x1000) {
            alloc_frame(get_page((int)i, true, kernel_directory), false, false);
        }
        if (placement_address>oldPlacement+0x1000) {
            System.out.println("Kernel Heap padding was not large enough");
            System.exit(0);
        }
        // Now, enable paging!
        switch_page_directory(kernel_directory);

        heap = new KernelHeap(this, kernel_directory, KHEAP_START, KHEAP_START+KHEAP_INITIAL_SIZE, KHEAP_END, false, false);
    }

    public void switch_page_directory(int dir) {
        current_directory = dir;
        CPU.CPU_SET_CRX(3, dir + PageDirectory.TABLES_PHYSICAL_OFFSET);
        CPU.CPU_SET_CRX(0, CPU.cpu.cr0 | CPU.CR0_PAGING); // Enable paging!
    }

    public int get_page(int address, boolean make, int dir) {
        // Turn the address into an index.
        address >>>= 12;
        // Find the page table containing this address.
        int table_idx = address >> 10;
        // dir->tables[idx]
        int tablePtr = Memory.phys_readd(dir + PageDirectory.TABLES_OFFSET + table_idx * PageDirectory.TABLES_ENTRY_SIZE);
        if (tablePtr != 0) { // If this table is already assigned
            return tablePtr + (address % 1024) * PageDirectory.TABLES_ENTRY_SIZE;
        } else if (make) {
            IntRef phys = new IntRef(0);
            tablePtr = kmalloc(PageDirectory.PAGE_TABLE_COUNT * PageDirectory.TABLES_ENTRY_SIZE, true, phys);
            Memory.mem_zero(tablePtr, PageDirectory.PAGE_TABLE_COUNT * PageDirectory.TABLES_ENTRY_SIZE);
            Memory.phys_writed(dir + PageDirectory.TABLES_OFFSET + table_idx * PageDirectory.TABLES_ENTRY_SIZE, tablePtr);

            Memory.phys_writed(dir + PageDirectory.TABLES_PHYSICAL_OFFSET + table_idx * PageDirectory.TABLES_PHYSICAL_SIZE, phys.value | 0x7); // PRESENT, RW, US.
            return tablePtr + (address % 1024) * PageDirectory.TABLES_ENTRY_SIZE;
        } else {
            return 0;
        }
    }

    public void registerPageFault(Interrupts interrupts) {
        interrupts.registerHandler(Interrupts.IRQ14, pageFaultHandler);
    }

    Callback.Handler pageFaultHandler = new Callback.Handler() {
        public int call() {
            System.out.println("Page Fault");
            System.exit(0);
            return 0;
        }

        public String getName() {
            return "PageFault";
        }
    };
}
