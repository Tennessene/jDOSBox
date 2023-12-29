package jdos.win.kernel;

import jdos.Dosbox;
import jdos.cpu.CPU;
import jdos.cpu.CPU_Regs;
import jdos.cpu.Callback;
import jdos.cpu.instructions.InstructionsTestCase;
import jdos.hardware.Memory;

public class testHeap extends InstructionsTestCase {
    KernelMemory memory;
    Interrupts interrupts;

    protected void setUp() throws Exception {
        super.setUp();
        CPU.cpu.code.big = true;

        CPU.CPU_SetSegGeneralCS(0);
        CPU.CPU_SetSegGeneralDS(0);
        CPU.CPU_SetSegGeneralES(0);
        CPU.CPU_SetSegGeneralFS(0);
        CPU.CPU_SetSegGeneralGS(0);
        CPU.CPU_SetSegGeneralSS(0);

        CPU.CPU_SET_CRX(0, CPU.cpu.cr0 | CPU.CR0_PROTECTION);
        CPU.cpu.pmode = true;

        memory = new KernelMemory();
        WinCallback.start(memory);
        interrupts = new Interrupts(memory);
        DescriptorTables descriptorTables = new DescriptorTables(interrupts, memory);
        memory.initialise_paging();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        Callback.inHandler = 0;
    }

    public void testInitialKernelHeap() {
        int p = memory.kmalloc(16);
        Memory.mem_writed(p, 0x12345678);
        assertEquals(0x56, Memory.mem_readb(p + 1));
    }

    public void testLargeAllocation() {
        int p = memory.kmalloc(1024*1024);
        Memory.mem_writed(p, 0xCDCDCDEF);
        assertEquals(0xCDCDCDEF, Memory.mem_readd(p));
        Memory.mem_writed(p+1024*1023, 0xABCDEF00);
        assertEquals(0xABCDEF00, Memory.mem_readd(p + 1024 * 1023));
        p = memory.kmalloc(16);
        Memory.mem_writed(p, 0xCDCDCDEF);
        assertEquals(0xCDCDCDEF, Memory.mem_readd(p));
        p = memory.kmalloc(1024*1024);
        Memory.mem_zero(p, 1024*1024);
        for (int i=0;i<1024*1024/4;i++) {
            assertEquals(0, Memory.mem_readd(p + i * 4));
            Memory.mem_writed(p+i*4, 0xABCDEF00);
            assertEquals(0xABCDEF00, Memory.mem_readd(p + i * 4));
        }
    }

    public void testPageFault() {
        CPU_Regs.reg_esp.dword = memory.kmalloc(1024)+1024;
        final KernelHeap heap = new KernelHeap(memory, memory.kernel_directory, 0xffff_ffff_d000_0000L, 0xffff_ffff_d000_1000L, 0xffff_ffff_d000_2000L, false, false);
        Callback.Handler cb = new Callback.Handler() {
            public int call() {
                int p = heap.alloc(0x1004, false);
                Memory.mem_writed(p+0x1000, 0x98765432);
                return 1; // !=0 will exit current loop and return from page fault core
            }

            public String getName() {
                return null;
            }
        };
        Callback.inHandler = 1; // make it run the core in place rather than throw an exception
        interrupts.registerHandler(14, cb);
        Dosbox.DOSBOX_SetNormalLoop();
        assertEquals(0x98765432, Memory.mem_readd(0xD0001000));
    }

    public void testFree() {
        int p = memory.kmalloc(4);
        Memory.mem_writed(p, 0xEFEFEFEF);
        assertEquals(0xEFEFEFEF, Memory.mem_readd(p));
        memory.kfree(p);
        int p1 = memory.kmalloc(8);
        // this point merged back in with the hold
        assertEquals(p1, p);
        int p3 = memory.kmalloc(8);
        memory.kfree(p1);
        int p4 = memory.kmalloc(16);
        // p1 should point to a empty 8 byte hold at the begging;
        assertTrue(p1!=p4);
        assertEquals(p1, memory.kmalloc(8));

        // all allocations fill up the begging of the memory
        assertEquals(1, memory.heap.getFreeItemCount());
        int p5 = memory.kmalloc(5);
        int p6 = memory.kmalloc(6);
        int p7 = memory.kmalloc(7);
        int p8 = memory.kmalloc(8);
        int p40k = memory.kmalloc(40*1024);
        int p9 = memory.kmalloc(9);

        // none of the following free's will combine with current free space
        assertEquals(1, memory.heap.getFreeItemCount());
        memory.kfree(p5);
        assertEquals(2, memory.heap.getFreeItemCount());
        memory.kfree(p7);
        assertEquals(3, memory.heap.getFreeItemCount());
        memory.kfree(p40k);
        assertEquals(4, memory.heap.getFreeItemCount());

        // free the last one, it should combine with the previous free space
        memory.kfree(p9);
        assertEquals(3, memory.heap.getFreeItemCount());

        // hole added to beginning
        memory.kfree(p1);
        assertEquals(4, memory.heap.getFreeItemCount());

        // hole combines with begging
        memory.kfree(p3);
        assertEquals(4, memory.heap.getFreeItemCount());

        // should eat up beginning hold
        int begin = memory.kmalloc(16);
        assertEquals(3, memory.heap.getFreeItemCount());
        assertEquals(begin, p1);
        memory.kfree(begin);
        assertEquals(4, memory.heap.getFreeItemCount());

        // combines with beginning and p5
        memory.kfree(p4);
        assertEquals(3, memory.heap.getFreeItemCount());

        memory.kfree(p6);
        assertEquals(2, memory.heap.getFreeItemCount());

        memory.kfree(p8);
        assertEquals(1, memory.heap.getFreeItemCount());
    }
}
