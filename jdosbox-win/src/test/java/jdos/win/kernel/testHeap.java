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
        Callback.inHandler = 0;
    }

    public void testInitialKernelHeap() {
        int p = memory.kmalloc(16);
        Memory.mem_writed(p, 0x12345678);
        assertTrue(Memory.mem_readb(p+1)==0x56);
    }

    public void testLargeAllocation() {
        int p = memory.kmalloc(1024*1024);
        Memory.mem_writed(p, 0xCDCDCDEF);
        assertTrue(Memory.mem_readd(p) == 0xCDCDCDEF);
        Memory.mem_writed(p+1024*1023, 0xABCDEF00);
        assertTrue(Memory.mem_readd(p+1024*1023) == 0xABCDEF00);
        p = memory.kmalloc(16);
        Memory.mem_writed(p, 0xCDCDCDEF);
        assertTrue(Memory.mem_readd(p) == 0xCDCDCDEF);
        p = memory.kmalloc(1024*1024);
        Memory.mem_zero(p, 1024*1024);
        for (int i=0;i<1024*1024/4;i++) {
            assertTrue(Memory.mem_readd(p+i*4)==0);
            Memory.mem_writed(p+i*4, 0xABCDEF00);
            assertTrue(Memory.mem_readd(p+i*4) == 0xABCDEF00);
        }
    }

    public void testPageFault() {
        CPU_Regs.reg_esp.dword = memory.kmalloc(1024)+1024;
        final KernelHeap heap = new KernelHeap(memory, memory.kernel_directory, 0xD0000000, 0xD0001000, 0xD0002000, false, false);
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
        assertTrue(Memory.mem_readd(0xD0001000)==0x98765432);
    }

    public void testFree() {
        int p = memory.kmalloc(4);
        Memory.mem_writed(p, 0xEFEFEFEF);
        assertTrue(Memory.mem_readd(p) == 0xEFEFEFEF);
        memory.kfree(p);
        int p1 = memory.kmalloc(8);
        // this point merged back in with the hold
        assertTrue(p1==p);
        int p3 = memory.kmalloc(8);
        memory.kfree(p1);
        int p4 = memory.kmalloc(16);
        // p1 should point to a empty 8 byte hold at the begging;
        assertTrue(p1!=p4);
        assertTrue(p1==memory.kmalloc(8));

        // all allocations fill up the begging of the memory
        assertTrue(memory.heap.getFreeItemCount()==1);
        int p5 = memory.kmalloc(5);
        int p6 = memory.kmalloc(6);
        int p7 = memory.kmalloc(7);
        int p8 = memory.kmalloc(8);
        int p40k = memory.kmalloc(40*1024);
        int p9 = memory.kmalloc(9);

        // none of the following free's will combine with current free space
        assertTrue(memory.heap.getFreeItemCount()==1);
        memory.kfree(p5);
        assertTrue(memory.heap.getFreeItemCount()==2);
        memory.kfree(p7);
        assertTrue(memory.heap.getFreeItemCount()==3);
        memory.kfree(p40k);
        assertTrue(memory.heap.getFreeItemCount()==4);

        // free the last one, it should combine with the previous free space
        memory.kfree(p9);
        assertTrue(memory.heap.getFreeItemCount()==3);

        // hole added to beginning
        memory.kfree(p1);
        assertTrue(memory.heap.getFreeItemCount()==4);

        // hole combines with begging
        memory.kfree(p3);
        assertTrue(memory.heap.getFreeItemCount()==4);

        // should eat up beginning hold
        int begin = memory.kmalloc(16);
        assertTrue(memory.heap.getFreeItemCount()==3);
        assertTrue(begin == p1);
        memory.kfree(begin);
        assertTrue(memory.heap.getFreeItemCount()==4);

        // combines with beginning and p5
        memory.kfree(p4);
        assertTrue(memory.heap.getFreeItemCount()==3);

        memory.kfree(p6);
        assertTrue(memory.heap.getFreeItemCount()==2);

        memory.kfree(p8);
        assertTrue(memory.heap.getFreeItemCount()==1);
    }
}
